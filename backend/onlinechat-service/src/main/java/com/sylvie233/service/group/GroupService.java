package com.sylvie233.service.group;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.common.enums.GroupRole;
import com.sylvie233.common.exception.BizException;
import com.sylvie233.repository.entity.GroupInfo;
import com.sylvie233.repository.entity.GroupMember;
import com.sylvie233.repository.mapper.GroupInfoMapper;
import com.sylvie233.repository.mapper.GroupMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 群组服务 — 创建/解散/设置/成员管理/邀请/踢人/管理员/群昵称
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService extends ServiceImpl<GroupInfoMapper, GroupInfo> {

    private final GroupInfoMapper groupInfoMapper;
    private final GroupMemberMapper groupMemberMapper;

    /**
     * 创建群 — 群主自动加入，role=OWNER
     */
    @Transactional
    public GroupInfo createGroup(Long ownerId, String groupName) {
        GroupInfo group = new GroupInfo();
        group.setGroupName(groupName);
        group.setOwnerId(ownerId);
        group.setMaxMembers(200);
        group.setMemberCount(1);
        group.setJoinType(0);
        group.setIsMutedAll(0);
        group.setStatus(0);
        save(group);

        GroupMember member = new GroupMember();
        member.setGroupId(group.getId());
        member.setUserId(ownerId);
        member.setRole(GroupRole.OWNER.getCode());
        member.setJoinTime(LocalDateTime.now());
        groupMemberMapper.insert(member);

        log.info("群创建成功: groupId={}, groupName={}, ownerId={}", group.getId(), groupName, ownerId);
        return group;
    }

    /**
     * 更新群设置 — 需要管理员/群主权限
     */
    @Transactional
    public void updateSettings(Long groupId, Long operatorId, GroupInfo body) {
        checkAdmin(groupId, operatorId);
        GroupInfo group = getById(groupId);
        if (body.getGroupName() != null) group.setGroupName(body.getGroupName());
        if (body.getAvatar() != null) group.setAvatar(body.getAvatar());
        if (body.getDescription() != null) group.setDescription(body.getDescription());
        if (body.getJoinType() != null) group.setJoinType(body.getJoinType());
        if (body.getMaxMembers() != null) group.setMaxMembers(body.getMaxMembers());
        if (body.getIsMutedAll() != null) group.setIsMutedAll(body.getIsMutedAll());
        updateById(group);
        log.info("群设置已更新: groupId={}, operator={}", groupId, operatorId);
    }

    /**
     * 解散群 — 仅群主
     */
    @Transactional
    public void dismissGroup(Long groupId, Long userId) {
        GroupInfo group = getById(groupId);
        if (group == null || !group.getOwnerId().equals(userId)) {
            throw BizException.of(403, "只有群主可以解散群");
        }
        group.setStatus(1);
        updateById(group);
        log.info("群已解散: groupId={}, ownerId={}", groupId, userId);
    }

    /** 获取群成员列表（按角色 + 加入时间排序） */
    public List<GroupMember> getMembers(Long groupId) {
        return groupMemberMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId)
                        .orderByAsc(GroupMember::getRole)
                        .orderByAsc(GroupMember::getJoinTime));
    }

    /** 加入群 */
    @Transactional
    public void joinGroup(Long groupId, Long userId) {
        GroupInfo group = getById(groupId);
        if (group == null || group.getStatus() == 1) throw BizException.of("群不存在或已解散");
        if (group.getMemberCount() >= group.getMaxMembers()) throw BizException.of("群已满");

        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(GroupRole.MEMBER.getCode());
        member.setJoinTime(LocalDateTime.now());
        groupMemberMapper.insert(member);

        group.setMemberCount(group.getMemberCount() + 1);
        updateById(group);
        log.info("用户加入群: groupId={}, userId={}", groupId, userId);
    }

    /** 邀请用户入群 — 需管理员权限 */
    @Transactional
    public void inviteMember(Long groupId, Long inviterId, Long inviteeId) {
        checkAdmin(groupId, inviterId);
        GroupMember exist = getMember(groupId, inviteeId);
        if (exist != null) throw BizException.of("该用户已在群内");

        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(inviteeId);
        member.setRole(GroupRole.MEMBER.getCode());
        member.setJoinTime(LocalDateTime.now());
        groupMemberMapper.insert(member);

        GroupInfo group = getById(groupId);
        group.setMemberCount(group.getMemberCount() + 1);
        updateById(group);
        log.info("邀请用户入群: groupId={}, inviter={}, invitee={}", groupId, inviterId, inviteeId);
    }

    /** 踢出成员 */
    @Transactional
    public void kickMember(Long groupId, Long operatorId, Long targetUserId) {
        checkAdmin(groupId, operatorId);
        GroupMember target = getMember(groupId, targetUserId);
        if (target == null) throw BizException.of("该用户不在群内");
        if (target.getRole() == GroupRole.OWNER.getCode()) throw BizException.of("不能踢出群主");

        groupMemberMapper.deleteById(target.getId());
        GroupInfo group = getById(groupId);
        group.setMemberCount(group.getMemberCount() - 1);
        updateById(group);
        log.info("踢出群成员: groupId={}, targetUserId={}, operator={}", groupId, targetUserId, operatorId);
    }

    /** 设置/取消管理员 — 仅群主可操作 */
    @Transactional
    public void setMemberRole(Long groupId, Long operatorId, Long targetUserId, Integer role) {
        GroupInfo group = getById(groupId);
        if (group == null || !group.getOwnerId().equals(operatorId)) {
            throw BizException.of(403, "只有群主可以设置管理员");
        }
        if (targetUserId.equals(operatorId)) throw BizException.of("不能修改自己的角色");

        GroupMember member = getMember(groupId, targetUserId);
        if (member == null) throw BizException.of("该用户不在群内");
        if (role != null && (role == 0 || role == 1)) {
            member.setRole(role);
            groupMemberMapper.updateById(member);
            log.info("设置成员角色: groupId={}, userId={}, newRole={}", groupId, targetUserId, role);
        }
    }

    /** 设置群内昵称 */
    @Transactional
    public void setMemberNickname(Long groupId, Long operatorId, Long targetUserId, String nickname) {
        GroupMember member = getMember(groupId, targetUserId);
        if (member == null) throw BizException.of("该用户不在群内");
        if (!operatorId.equals(targetUserId)) checkAdmin(groupId, operatorId);
        member.setNicknameInGroup(nickname);
        groupMemberMapper.updateById(member);
    }

    /** 更新成员设置（免打扰/置顶） */
    @Transactional
    public void updateMemberSettings(Long groupId, Long userId, Map<String, Object> body) {
        GroupMember member = getMember(groupId, userId);
        if (member == null) throw BizException.of("你不在该群内");
        if (body.containsKey("isMuted")) member.setIsMuted((Integer) body.get("isMuted"));
        if (body.containsKey("isPinned")) member.setIsPinned((Integer) body.get("isPinned"));
        groupMemberMapper.updateById(member);
    }

    // ==================== 内部 ====================

    private void checkAdmin(Long groupId, Long userId) {
        GroupMember member = getMember(groupId, userId);
        if (member == null || member.getRole() < GroupRole.ADMIN.getCode()) {
            log.warn("无权限操作: groupId={}, userId={}, role={}",
                    groupId, userId, member != null ? member.getRole() : null);
            throw BizException.of(403, "无操作权限");
        }
    }

    private GroupMember getMember(Long groupId, Long userId) {
        return groupMemberMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, userId));
    }
}
