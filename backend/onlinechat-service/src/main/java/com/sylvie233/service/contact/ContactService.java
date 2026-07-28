package com.sylvie233.service.contact;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.common.exception.BizException;
import com.sylvie233.repository.entity.Blocklist;
import com.sylvie233.repository.entity.Contact;
import com.sylvie233.repository.entity.ContactGroup;
import com.sylvie233.repository.entity.FriendRequest;
import com.sylvie233.repository.entity.UserSetting;
import com.sylvie233.repository.mapper.BlocklistMapper;
import com.sylvie233.repository.mapper.ContactGroupMapper;
import com.sylvie233.repository.mapper.ContactMapper;
import com.sylvie233.repository.mapper.FriendRequestMapper;
import com.sylvie233.repository.mapper.UserSettingMapper;
import com.sylvie233.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 联系人服务 — 好友关系、好友申请、黑名单
 * <p>完整的好友生命周期管理：分组/添加/删除/星标/备注/移动分组/黑名单</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService extends ServiceImpl<ContactMapper, Contact> {

    private final ContactMapper contactMapper;
    private final ContactGroupMapper contactGroupMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final BlocklistMapper blocklistMapper;
    private final UserSettingMapper userSettingMapper;
    private final NotificationService notificationService;

    // ==================== 好友分组 ====================

    /** 获取用户的所有好友分组 */
    public List<ContactGroup> getGroups(Long userId) {
        return contactGroupMapper.selectList(
                new LambdaQueryWrapper<ContactGroup>()
                        .eq(ContactGroup::getUserId, userId)
                        .orderByAsc(ContactGroup::getSortOrder));
    }

    /** 创建好友分组 */
    @Transactional
    public ContactGroup createGroup(Long userId, String groupName) {
        ContactGroup group = new ContactGroup();
        group.setUserId(userId);
        group.setGroupName(groupName);
        contactGroupMapper.insert(group);
        log.info("创建好友分组: userId={}, groupName={}", userId, groupName);
        return group;
    }

    /** 重命名分组 */
    @Transactional
    public void renameGroup(Long groupId, Long userId, String groupName) {
        ContactGroup group = contactGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw BizException.of(403, "无权操作");
        }
        group.setGroupName(groupName);
        contactGroupMapper.updateById(group);
        log.info("重命名分组: groupId={}, newName={}", groupId, groupName);
    }

    /** 删除分组 — 好友移回默认分组(groupId=0) */
    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        ContactGroup group = contactGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw BizException.of(403, "无权操作");
        }
        // 分组下好友移回默认分组
        lambdaUpdate().eq(Contact::getUserId, userId)
                .eq(Contact::getGroupId, groupId)
                .set(Contact::getGroupId, 0L)
                .update();
        contactGroupMapper.deleteById(groupId);
        log.info("删除分组: groupId={}, userId={}", groupId, userId);
    }

    // ==================== 好友管理 ====================

    /** 获取好友列表 */
    public List<Contact> getContacts(Long userId) {
        return lambdaQuery().eq(Contact::getUserId, userId).list();
    }

    /** 删除好友（双向删除） */
    @Transactional
    public void deleteContact(Long userId, Long contactUserId) {
        lambdaUpdate().eq(Contact::getUserId, userId)
                .eq(Contact::getContactUserId, contactUserId).remove();
        lambdaUpdate().eq(Contact::getUserId, contactUserId)
                .eq(Contact::getContactUserId, userId).remove();
        log.info("删除好友: {} <-> {}", userId, contactUserId);
    }

    /** 修改好友备注 */
    @Transactional
    public void updateRemark(Long userId, Long contactUserId, String remark) {
        lambdaUpdate().eq(Contact::getUserId, userId)
                .eq(Contact::getContactUserId, contactUserId)
                .set(Contact::getRemark, remark).update();
        log.debug("修改备注: userId={}, contactUserId={}, remark={}", userId, contactUserId, remark);
    }

    /** 星标/取消星标好友 */
    @Transactional
    public void toggleStar(Long userId, Long contactUserId, boolean starred) {
        lambdaUpdate().eq(Contact::getUserId, userId)
                .eq(Contact::getContactUserId, contactUserId)
                .set(Contact::getIsStarred, starred ? 1 : 0).update();
    }

    /** 移动好友到分组 */
    @Transactional
    public void moveToGroup(Long userId, Long contactUserId, Long groupId) {
        lambdaUpdate().eq(Contact::getUserId, userId)
                .eq(Contact::getContactUserId, contactUserId)
                .set(Contact::getGroupId, groupId).update();
    }

    // ==================== 好友申请 ====================

    @Transactional
    public FriendRequest sendRequest(Long fromUserId, Long toUserId, String verifyMessage) {
        return sendRequest(fromUserId, toUserId, verifyMessage, "");
    }

    /**
     * 发送好友申请 — 检查好友关系、黑名单、对方验证策略
     */
    @Transactional
    public FriendRequest sendRequest(Long fromUserId, Long toUserId, String verifyMessage, String source) {
        // 检查是否已是好友
        Contact exist = lambdaQuery().eq(Contact::getUserId, fromUserId)
                .eq(Contact::getContactUserId, toUserId).one();
        if (exist != null) throw BizException.of("已经是好友了");

        // 检查是否有待处理的申请
        FriendRequest pending = friendRequestMapper.selectOne(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getFromUserId, fromUserId)
                        .eq(FriendRequest::getToUserId, toUserId)
                        .eq(FriendRequest::getStatus, 0));
        if (pending != null) throw BizException.of("已有待处理的好友申请");

        // 检查黑名单
        Blocklist blocked;
        blocked = blocklistMapper.selectOne(
                new LambdaQueryWrapper<Blocklist>()
                        .eq(Blocklist::getUserId, toUserId).eq(Blocklist::getBlockedUserId, fromUserId));
        if (blocked != null) throw BizException.of("对方已将你拉黑");

        // 检查对方的验证策略
        UserSetting setting = userSettingMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSetting>()
                        .eq(UserSetting::getUserId, toUserId));
        if (setting != null && setting.getFriendVerifyType() != null) {
            if (setting.getFriendVerifyType() == 2) {
                throw BizException.of("对方拒绝所有人添加好友");
            }
            if (setting.getFriendVerifyType() == 0) {
                // 无需验证，直接建立好友
                addContact(fromUserId, toUserId, source);
                addContact(toUserId, fromUserId, source);
                FriendRequest autoReq = new FriendRequest();
                autoReq.setFromUserId(fromUserId);
                autoReq.setToUserId(toUserId);
                autoReq.setStatus(1); // 插入一条已同意的请求
                autoReq.setHandledTime(LocalDateTime.now());
                autoReq.setRemark(source);
                friendRequestMapper.insert(autoReq);
                log.info("好友验证策略=0, 直接建立好友: {} <-> {}", fromUserId, toUserId);
                return autoReq;
            }
        }

        FriendRequest req = new FriendRequest();
        req.setFromUserId(fromUserId);
        req.setToUserId(toUserId);
        req.setVerifyMessage(verifyMessage);
        req.setRemark(source);
        req.setStatus(0); // 插入一条待同意的请求
        friendRequestMapper.insert(req);
        log.info("发送好友申请: from={}, to={}", fromUserId, toUserId);

        // 给接收方生成通知
        notificationService.send(toUserId, 1,
                "新的好友申请", "用户" + fromUserId + " 请求添加你为好友", req.getId());
        return req;
    }

    /** 获取待处理的好友申请 */
    public List<FriendRequest> getPendingRequests(Long userId) {
        return friendRequestMapper.selectList(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getToUserId, userId)
                        .eq(FriendRequest::getStatus, 0)
                        .orderByDesc(FriendRequest::getCreateTime));
    }

    /** 处理好友申请（同意/拒绝） */
    @Transactional
    public void handleRequest(Long requestId, Long handlerId, boolean agree) {
        FriendRequest req = friendRequestMapper.selectById(requestId);
        if (req == null || !req.getToUserId().equals(handlerId)) {
            throw BizException.of("无权处理该申请");
        }
        if (req.getStatus() != 0) throw BizException.of("该申请已处理");

        req.setStatus(agree ? 1 : 2);
        req.setHandledTime(LocalDateTime.now());
        friendRequestMapper.updateById(req);

        if (agree) {
            String source = req.getRemark() != null ? req.getRemark() : "";
            addContact(req.getFromUserId(), req.getToUserId(), source);
            addContact(req.getToUserId(), req.getFromUserId(), source);
            // 给申请发出方通知
            notificationService.send(req.getFromUserId(), 1,
                    "好友申请已通过", "用户" + handlerId + " 已同意你的好友申请", req.getId());
        }
        log.info("处理好友申请: requestId={}, agree={}", requestId, agree);
    }

    private void addContact(Long userId, Long contactUserId, String source) {
        Contact contact = new Contact();
        contact.setUserId(userId);
        contact.setContactUserId(contactUserId);
        contact.setSource(source);
        contactMapper.insert(contact);
    }

    // ==================== 黑名单 ====================

    @Transactional
    public void blockUser(Long userId, Long blockedUserId) {
        blockUser(userId, blockedUserId, null);
    }

    /** 拉黑用户 — 同时删除好友关系 */
    @Transactional
    public void blockUser(Long userId, Long blockedUserId, String reason) {
        Blocklist exist;
        exist = blocklistMapper.selectOne(
                new LambdaQueryWrapper<Blocklist>()
                        .eq(Blocklist::getUserId, userId).eq(Blocklist::getBlockedUserId, blockedUserId));
        if (exist != null) return;

        Blocklist block = new Blocklist();
        block.setUserId(userId);
        block.setBlockedUserId(blockedUserId);
        block.setReason(reason);
        blocklistMapper.insert(block);
        deleteContact(userId, blockedUserId);
        log.info("拉黑用户: userId={}, blockedUserId={}, reason={}", userId, blockedUserId, reason);
    }

    /** 取消拉黑 */
    @Transactional
    public void unblockUser(Long userId, Long blockedUserId) {
        blocklistMapper.delete(
                new LambdaQueryWrapper<Blocklist>()
                        .eq(Blocklist::getUserId, userId).eq(Blocklist::getBlockedUserId, blockedUserId));
        log.info("取消拉黑: userId={}, blockedUserId={}", userId, blockedUserId);
    }

    /** 获取黑名单列表 */
    public List<Blocklist> getBlocklist(Long userId) {
        return blocklistMapper.selectList(
                new LambdaQueryWrapper<Blocklist>()
                        .eq(Blocklist::getUserId, userId).orderByDesc(Blocklist::getCreateTime));
    }
}
