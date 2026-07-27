package com.sylvie233.service.group;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.common.exception.BizException;
import com.sylvie233.repository.entity.GroupInfo;
import com.sylvie233.repository.entity.GroupMember;
import com.sylvie233.repository.entity.GroupRequest;
import com.sylvie233.repository.mapper.GroupInfoMapper;
import com.sylvie233.repository.mapper.GroupMemberMapper;
import com.sylvie233.repository.mapper.GroupRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群申请/邀请服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupRequestService extends ServiceImpl<GroupRequestMapper, GroupRequest> {

    private final GroupRequestMapper groupRequestMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupInfoMapper groupInfoMapper;

    /**
     * 申请加入群
     */
    @Transactional
    public GroupRequest applyJoin(Long groupId, Long userId, String verifyMessage) {
        // 检查是否已在群内
        GroupMember exist = groupMemberMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId)
                        .eq(GroupMember::getUserId, userId));
        if (exist != null) {
            throw BizException.of("你已在群内");
        }

        // 检查是否有待处理的申请
        GroupRequest pending = lambdaQuery()
                .eq(GroupRequest::getGroupId, groupId)
                .eq(GroupRequest::getFromUserId, userId)
                .eq(GroupRequest::getStatus, 0)
                .one();
        if (pending != null) {
            throw BizException.of("已有待处理的申请");
        }

        GroupRequest request = new GroupRequest();
        request.setGroupId(groupId);
        request.setFromUserId(userId);
        request.setType(0); // 0=申请入群
        request.setVerifyMessage(verifyMessage);
        request.setStatus(0);
        save(request);
        return request;
    }

    /**
     * 邀请用户入群
     */
    @Transactional
    public GroupRequest inviteUser(Long groupId, Long inviterId, Long inviteeId, String verifyMessage) {
        GroupMember exist = groupMemberMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId)
                        .eq(GroupMember::getUserId, inviteeId));
        if (exist != null) {
            throw BizException.of("该用户已在群内");
        }

        GroupRequest request = new GroupRequest();
        request.setGroupId(groupId);
        request.setFromUserId(inviterId);
        request.setToUserId(inviteeId);
        request.setType(1); // 1=邀请入群
        request.setVerifyMessage(verifyMessage);
        request.setStatus(0);
        save(request);
        return request;
    }

    /**
     * 处理群申请/邀请
     */
    @Transactional
    public void handleRequest(Long requestId, Long handlerId, boolean agree) {
        GroupRequest request = getById(requestId);
        if (request == null || request.getStatus() != 0) {
            throw BizException.of("该请求已处理或不存在");
        }

        request.setStatus(agree ? 1 : 2);
        request.setHandledTime(LocalDateTime.now());
        updateById(request);

        if (agree) {
            Long userId = request.getType() == 0 ? request.getFromUserId() : request.getToUserId();
            GroupMember member = new GroupMember();
            member.setGroupId(request.getGroupId());
            member.setUserId(userId);
            member.setRole(0);
            member.setJoinTime(LocalDateTime.now());
            groupMemberMapper.insert(member);

            GroupInfo group = groupInfoMapper.selectById(request.getGroupId());
            if (group != null) {
                group.setMemberCount(group.getMemberCount() + 1);
                groupInfoMapper.updateById(group);
            }
        }
    }

    /**
     * 获取群申请列表
     */
    public List<GroupRequest> getRequests(Long groupId) {
        return lambdaQuery()
                .eq(GroupRequest::getGroupId, groupId)
                .eq(GroupRequest::getStatus, 0)
                .orderByDesc(GroupRequest::getCreateTime)
                .list();
    }

    /**
     * 获取用户收到的入群邀请
     */
    public List<GroupRequest> getInvitations(Long userId) {
        return lambdaQuery()
                .eq(GroupRequest::getToUserId, userId)
                .eq(GroupRequest::getType, 1)
                .orderByDesc(GroupRequest::getCreateTime)
                .list();
    }
}
