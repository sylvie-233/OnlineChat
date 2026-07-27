package com.sylvie233.task.event;

import com.sylvie233.repository.entity.Conversation;
import com.sylvie233.repository.entity.GroupMember;
import com.sylvie233.repository.entity.Message;
import com.sylvie233.repository.mapper.GroupMemberMapper;
import com.sylvie233.service.cache.RedisCacheService;
import com.sylvie233.service.conversation.ConversationService;
import com.sylvie233.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 消息事件监听器 — 事务提交后异步执行：
 * 1. 更新会话（最后消息 + 未读计数）
 * 2. 生成 @提及通知
 * 3. 新消息推送通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventListener {

    private final ConversationService conversationService;
    private final NotificationService notificationService;
    private final RedisCacheService redisCacheService;
    private final GroupMemberMapper groupMemberMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageEvent(MessageEvent event) {
        Message msg = event.getMessage();
        log.info("异步处理消息: msgId={}, type={}", msg.getId(), msg.getMsgType());

        if (msg.getConversationType() == null) return;

        if (msg.getConversationType() == 0) {
            updatePrivateConversations(msg);
        } else if (msg.getConversationType() == 1) {
            updateGroupConversations(msg);
        }

        // 单聊新消息通知
        if (msg.getConversationType() == 0) {
            notificationService.send(msg.getToId(), 4,
                    "新消息", truncate(msg.getContent(), 50), msg.getId());
        }
    }

    private void updatePrivateConversations(Message msg) {
        conversationService.updateLastMessage(
                buildConv(msg.getToId(), 0, msg.getFromUserId(), msg));
        conversationService.updateLastMessage(
                buildConv(msg.getFromUserId(), 0, msg.getToId(), msg));
    }

    private void updateGroupConversations(Message msg) {
        List<GroupMember> members = groupMemberMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, msg.getToId()));
        for (GroupMember member : members) {
            if (member.getUserId().equals(msg.getFromUserId())) continue;
            conversationService.updateLastMessage(
                    buildConv(member.getUserId(), 1, msg.getToId(), msg));
        }
    }

    private Conversation buildConv(Long userId, int type, Long targetId, Message msg) {
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setType(type);
        conv.setTargetId(targetId);
        conv.setLastMessageId(msg.getId());
        conv.setLastMessageSeq(msg.getSeq());
        return conv;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
