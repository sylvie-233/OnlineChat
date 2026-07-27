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

/**
 * 群组服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService extends ServiceImpl<GroupInfoMapper, GroupInfo> {

    private final GroupInfoMapper groupInfoMapper;
    private final GroupMemberMapper groupMemberMapper;

    /**
     * 创建群
     */
    @Transactional
    public GroupInfo createGroup(Long ownerId, String groupName) {
        GroupInfo group = new GroupInfo();
        group.setGroupName(groupName);
        group.setOwnerId(ownerId);
        group.setMemberCount(1);
        save(group);

        // 群主自动加入
        GroupMember member = new GroupMember();
        member.setGroupId(group.getId());
        member.setUserId(ownerId);
        member.setRole(GroupRole.OWNER.getCode());
        member.setJoinTime(LocalDateTime.now());
        groupMemberMapper.insert(member);

        return group;
    }

    /**
     * 成员加入群
     */
    @Transactional
    public void joinGroup(Long groupId, Long userId) {
        GroupInfo group = getById(groupId);
        if (group == null || group.getStatus() == 1) {
            throw BizException.of("群不存在或已解散");
        }
        if (group.getMemberCount() >= group.getMaxMembers()) {
            throw BizException.of("群已满");
        }

        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(GroupRole.MEMBER.getCode());
        member.setJoinTime(LocalDateTime.now());
        groupMemberMapper.insert(member);

        // 更新群成员数
        group.setMemberCount(group.getMemberCount() + 1);
        updateById(group);
    }

    /**
     * 踢出成员
     */
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
    }

    /**
     * 检查是否为管理员及以上
     */
    private void checkAdmin(Long groupId, Long userId) {
        GroupMember member = getMember(groupId, userId);
        if (member == null || member.getRole() < GroupRole.ADMIN.getCode()) {
            throw BizException.of(403, "无操作权限");
        }
    }

    private GroupMember getMember(Long groupId, Long userId) {
        return groupMemberMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId)
                        .eq(GroupMember::getUserId, userId));
    }
}
