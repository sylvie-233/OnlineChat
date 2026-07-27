package com.sylvie233.service.contact;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.common.exception.BizException;
import com.sylvie233.repository.entity.Blocklist;
import com.sylvie233.repository.entity.Contact;
import com.sylvie233.repository.entity.ContactGroup;
import com.sylvie233.repository.entity.FriendRequest;
import com.sylvie233.repository.mapper.BlocklistMapper;
import com.sylvie233.repository.mapper.ContactGroupMapper;
import com.sylvie233.repository.mapper.ContactMapper;
import com.sylvie233.repository.mapper.FriendRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 联系人服务 — 好友关系、好友申请、黑名单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService extends ServiceImpl<ContactMapper, Contact> {

    private final ContactMapper contactMapper;
    private final ContactGroupMapper contactGroupMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final BlocklistMapper blocklistMapper;

    // ==================== 好友分组 ====================

    /**
     * 获取用户的所有好友分组
     */
    public List<ContactGroup> getGroups(Long userId) {
        return contactGroupMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ContactGroup>()
                        .eq(ContactGroup::getUserId, userId)
                        .orderByAsc(ContactGroup::getSortOrder));
    }

    /**
     * 创建好友分组
     */
    @Transactional
    public ContactGroup createGroup(Long userId, String groupName) {
        ContactGroup group = new ContactGroup();
        group.setUserId(userId);
        group.setGroupName(groupName);
        contactGroupMapper.insert(group);
        return group;
    }

    // ==================== 好友管理 ====================

    /**
     * 获取用户的好友列表
     */
    public List<Contact> getContacts(Long userId) {
        return lambdaQuery().eq(Contact::getUserId, userId).list();
    }

    /**
     * 删除好友（双向删除）
     */
    @Transactional
    public void deleteContact(Long userId, Long contactUserId) {
        lambdaUpdate()
                .eq(Contact::getUserId, userId)
                .eq(Contact::getContactUserId, contactUserId)
                .remove();
        lambdaUpdate()
                .eq(Contact::getUserId, contactUserId)
                .eq(Contact::getContactUserId, userId)
                .remove();
    }

    /**
     * 修改备注
     */
    @Transactional
    public void updateRemark(Long userId, Long contactUserId, String remark) {
        lambdaUpdate()
                .eq(Contact::getUserId, userId)
                .eq(Contact::getContactUserId, contactUserId)
                .set(Contact::getRemark, remark)
                .update();
    }

    // ==================== 好友申请 ====================

    /**
     * 发送好友申请
     */
    @Transactional
    public FriendRequest sendRequest(Long fromUserId, Long toUserId, String verifyMessage) {
        // 检查是否已是好友
        Contact exist = lambdaQuery()
                .eq(Contact::getUserId, fromUserId)
                .eq(Contact::getContactUserId, toUserId)
                .one();
        if (exist != null) {
            throw BizException.of("已经是好友了");
        }
        // 检查黑名单
        Blocklist blocked = blocklistMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Blocklist>()
                        .eq(Blocklist::getUserId, toUserId)
                        .eq(Blocklist::getBlockedUserId, fromUserId));
        if (blocked != null) {
            throw BizException.of("对方已将你拉黑");
        }

        FriendRequest req = new FriendRequest();
        req.setFromUserId(fromUserId);
        req.setToUserId(toUserId);
        req.setVerifyMessage(verifyMessage);
        req.setStatus(0);
        friendRequestMapper.insert(req);
        return req;
    }

    /**
     * 处理好友申请
     */
    @Transactional
    public void handleRequest(Long requestId, Long handlerId, boolean agree) {
        FriendRequest req = friendRequestMapper.selectById(requestId);
        if (req == null || !req.getToUserId().equals(handlerId)) {
            throw BizException.of("无权处理该申请");
        }
        if (req.getStatus() != 0) {
            throw BizException.of("该申请已处理");
        }

        req.setStatus(agree ? 1 : 2);
        req.setHandledTime(LocalDateTime.now());
        friendRequestMapper.updateById(req);

        if (agree) {
            // 双向添加好友
            addContact(req.getFromUserId(), req.getToUserId());
            addContact(req.getToUserId(), req.getFromUserId());
        }
    }

    private void addContact(Long userId, Long contactUserId) {
        Contact contact = new Contact();
        contact.setUserId(userId);
        contact.setContactUserId(contactUserId);
        contactMapper.insert(contact);
    }

    // ==================== 黑名单 ====================

    /**
     * 拉黑用户
     */
    @Transactional
    public void blockUser(Long userId, Long blockedUserId) {
        Blocklist exist = blocklistMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Blocklist>()
                        .eq(Blocklist::getUserId, userId)
                        .eq(Blocklist::getBlockedUserId, blockedUserId));
        if (exist != null) return;

        Blocklist block = new Blocklist();
        block.setUserId(userId);
        block.setBlockedUserId(blockedUserId);
        blocklistMapper.insert(block);

        // 删除好友关系
        deleteContact(userId, blockedUserId);
    }

    /**
     * 取消拉黑
     */
    @Transactional
    public void unblockUser(Long userId, Long blockedUserId) {
        blocklistMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Blocklist>()
                        .eq(Blocklist::getUserId, userId)
                        .eq(Blocklist::getBlockedUserId, blockedUserId));
    }
}
