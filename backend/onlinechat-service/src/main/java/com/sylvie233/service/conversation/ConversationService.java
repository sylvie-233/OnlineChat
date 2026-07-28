package com.sylvie233.service.conversation;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.repository.entity.Conversation;
import com.sylvie233.repository.entity.GroupInfo;
import com.sylvie233.repository.entity.User;
import com.sylvie233.repository.mapper.ConversationMapper;
import com.sylvie233.repository.mapper.GroupInfoMapper;
import com.sylvie233.repository.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 会话服务 — 管理用户聊天列表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService extends ServiceImpl<ConversationMapper, Conversation> {

    private final ConversationMapper conversationMapper;
    private final UserMapper userMapper;
    private final GroupInfoMapper groupInfoMapper;

    /** 获取用户聊天列表（置顶优先 + 最后消息时间排序，排除隐藏） */
    public List<Conversation> getConversations(Long userId) {
        List<Conversation> list = lambdaQuery()
                .eq(Conversation::getUserId, userId)
                .eq(Conversation::getIsHidden, 0)
                .orderByDesc(Conversation::getIsPinned)
                .orderByDesc(Conversation::getUpdateTime)
                .list();

        // 填充 targetName / targetAvatar
        for (Conversation c : list) {
            if (c.getType() == 0) {
                // 单聊 — 查对方用户
                User target = userMapper.selectById(c.getTargetId());
                if (target != null) {
                    c.setTargetName(target.getNickname() != null ? target.getNickname() : target.getUsername());
                    c.setTargetAvatar(target.getAvatar() != null ? target.getAvatar() : "");
                } else {
                    c.setTargetName("用户" + c.getTargetId());
                }
            } else {
                // 群聊 — 查群信息
                GroupInfo group = groupInfoMapper.selectById(c.getTargetId());
                if (group != null) {
                    c.setTargetName(group.getGroupName());
                    c.setTargetAvatar(group.getAvatar() != null ? group.getAvatar() : "");
                } else {
                    c.setTargetName("群聊" + c.getTargetId());
                }
            }
        }
        return list;
    }

    /** 检查会话是否属于指定用户 */
    public boolean isOwner(Long conversationId, Long userId) {
        Conversation conv = getById(conversationId);
        return conv != null && conv.getUserId().equals(userId);
    }

    /**
     * 原子更新 — 更新最后消息 + 可选的未读计数+1
     */
    @Transactional
    public void updateLastMessage(Conversation conv, boolean incrementUnread) {
        Conversation exist = lambdaQuery()
                .eq(Conversation::getUserId, conv.getUserId())
                .eq(Conversation::getType, conv.getType())
                .eq(Conversation::getTargetId, conv.getTargetId())
                .one();

        if (exist != null) {
            var updater = lambdaUpdate()
                    .eq(Conversation::getId, exist.getId())
                    .set(Conversation::getLastMessageId, conv.getLastMessageId())
                    .set(Conversation::getLastMessageSeq, conv.getLastMessageSeq());
            if (incrementUnread) {
                updater.setSql("unread_count = unread_count + 1");
            }
            updater.update();
        } else {
            conv.setUnreadCount(incrementUnread ? 1 : 0);
            save(conv);
        }
    }

    @Transactional
    public void clearUnread(Long conversationId) {
        lambdaUpdate().eq(Conversation::getId, conversationId)
                .set(Conversation::getUnreadCount, 0).update();
    }

    @Transactional
    public void setPinned(Long conversationId, boolean pinned) {
        lambdaUpdate().eq(Conversation::getId, conversationId)
                .set(Conversation::getIsPinned, pinned ? 1 : 0).update();
    }

    @Transactional
    public void setMuted(Long conversationId, boolean muted) {
        lambdaUpdate().eq(Conversation::getId, conversationId)
                .set(Conversation::getIsMuted, muted ? 1 : 0).update();
    }

    @Transactional
    public void saveDraft(Long conversationId, String draft) {
        lambdaUpdate().eq(Conversation::getId, conversationId)
                .set(Conversation::getDraft, draft).update();
    }

    @Transactional
    public void setHidden(Long conversationId, boolean hidden) {
        lambdaUpdate().eq(Conversation::getId, conversationId)
                .set(Conversation::getIsHidden, hidden ? 1 : 0).update();
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        removeById(conversationId);
    }
}
