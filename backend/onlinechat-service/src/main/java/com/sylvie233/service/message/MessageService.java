package com.sylvie233.service.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.common.enums.ConversationType;
import com.sylvie233.common.enums.MessageStatus;
import com.sylvie233.repository.entity.Conversation;
import com.sylvie233.repository.entity.GroupMember;
import com.sylvie233.repository.entity.Message;
import com.sylvie233.repository.entity.MessageRecall;
import com.sylvie233.repository.mapper.MessageMapper;
import com.sylvie233.repository.mapper.MessageRecallMapper;
import com.sylvie233.service.cache.RedisCacheService;
import com.sylvie233.service.conversation.ConversationService;
import com.sylvie233.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息核心服务 — seq自增、状态机流转、幂等去重、@提及解析、消息重发
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService extends ServiceImpl<MessageMapper, Message> {

    private final MessageMapper messageMapper;
    private final MessageRecallMapper messageRecallMapper;
    private final MessageMentionService mentionService;
    private final RedisCacheService redisCacheService;
    private final MessageQueueProducer queueProducer;
    private final NotificationService notificationService;
    private final ConversationService conversationService;
    private final com.sylvie233.repository.mapper.ConversationMapper conversationMapper;
    private final com.sylvie233.repository.mapper.GroupMemberMapper groupMemberMapper;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\{(\\d+)\\}");

    /**
     * 发送消息 — 幂等去重、seq 自增、@提及解析
     * <p>模式由 im.queue.enabled 控制：</p>
     * <ul>
     *   <li>队列模式: 发布到 Redis Stream → 异步批量入库（高并发削峰）</li>
     *   <li>直写模式: 直接落库（低延迟）</li>
     * </ul>
     *
     * @param msg 消息实体（需含 clientMsgId / conversationType / conversationId）
     * @return 持久化后的消息（含自动生成的 seq 和 id）
     */
    @Transactional
    public Message sendMessage(Message msg) {
        // 1. 幂等检查
        if (msg.getClientMsgId() != null) {
            Message exist = lambdaQuery().eq(Message::getClientMsgId, msg.getClientMsgId()).one();
            if (exist != null) {
                log.warn("重复消息幂等拦截: clientMsgId={}", msg.getClientMsgId());
                return exist;
            }
        }

        // 2. 确保 conversationId 正确（查已有会话或新建）
        if (msg.getConversationId() == null || msg.getConversationId() == 0L) {
            msg.setConversationId(resolveConversationId(msg));
        }

        // 3. seq 自增
        String seqKey = buildSeqKey(msg);
        msg.setSeq(redisCacheService.nextSeq(seqKey));
        msg.setStatus(MessageStatus.SENT.getCode());
        msg.setSendTime(LocalDateTime.now());

        // 3. 队列模式 or 直写模式
        String recordId = queueProducer.publish(msg);
        if (recordId != null) {
            // 队列模式：消息已入 Redis Stream，由消费者异步批量入库
            log.debug("消息入队成功: msgId={}, recordId={}", msg.getId(), recordId);
        } else {
            // 直写模式：直接 MySQL
            save(msg);
            log.info("消息直写成功: msgId={}, convType={}, convId={}, from={}, to={}",
                    msg.getId(), msg.getConversationType(),
                    msg.getConversationId(), msg.getFromUserId(), msg.getToId());
        }

        // 4. 解析 @提及
        parseAndCreateMentions(msg);

        // 5. 更新双方会话（最后消息 + 未读计数）
        updateConversation(msg);

        // 6. 单聊时给接收方生成通知
        if (msg.getConversationType() != null && msg.getConversationType() == 0) {
            notificationService.send(msg.getToId(), 4,
                    "新消息", truncateContent(msg.getContent()), msg.getId());
        }

        return msg;
    }

    /**
     * 重发失败的消息
     *
     * @return 新消息，失败返回 null
     */
    @Transactional
    public Message retryMessage(Long messageId, Long userId) {
        Message msg = getById(messageId);
        if (msg == null || !msg.getFromUserId().equals(userId)) {
            log.warn("无权重发: msgId={}, userId={}", messageId, userId);
            return null;
        }
        if (msg.getStatus() != MessageStatus.FAILED.getCode()) {
            log.warn("只能重发失败消息: msgId={}, status={}", messageId, msg.getStatus());
            return null;
        }
        // 重新发送（生成新 seq）
        msg.setStatus(MessageStatus.SENT.getCode());
        msg.setSendTime(LocalDateTime.now());
        String seqKey = buildSeqKey(msg);
        msg.setSeq(redisCacheService.nextSeq(seqKey));
        updateById(msg);
        log.info("消息重发: msgId={}, newSeq={}", messageId, msg.getSeq());
        return msg;
    }

    /**
     * 标记消息发送失败
     */
    @Transactional
    public void markFailed(Long messageId, String reason) {
        lambdaUpdate()
                .eq(Message::getId, messageId)
                .set(Message::getStatus, MessageStatus.FAILED.getCode())
                .update();
        log.warn("消息发送失败: msgId={}, reason={}", messageId, reason);
    }

    /**
     * 更新消息状态（送达/已读等）
     */
    @Transactional
    public void updateStatus(Long messageId, MessageStatus status) {
        lambdaUpdate().eq(Message::getId, messageId)
                .set(Message::getStatus, status.getCode()).update();
    }

    /**
     * 批量标记会话内消息为已送达
     */
    @Transactional
    public void markDelivered(Long conversationId, Integer conversationType, Long toUserId) {
        lambdaUpdate()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getConversationType, conversationType)
                .eq(Message::getToId, toUserId)
                .eq(Message::getStatus, MessageStatus.SENT.getCode())
                .set(Message::getStatus, MessageStatus.DELIVERED.getCode())
                .update();
        log.debug("批量标记已送达: convId={}, type={}, toUser={}", conversationId, conversationType, toUserId);
    }

    /**
     * 拉取会话最新 N 条消息（私聊按用户对双查，群聊按 conversationId）
     */
    public List<Message> getLatestMessages(Long conversationId, ConversationType type, int limit, Long currentUserId) {
        Long targetId = resolveTargetId(conversationId, type, currentUserId);
        return messageMapper.selectLatestByConversation(conversationId, type.getCode(), currentUserId, targetId, limit);
    }

    /**
     * 向前翻页
     */
    public List<Message> getHistoryMessages(Long conversationId, ConversationType type,
                                             String cursorTime, int limit, Long currentUserId) {
        Long targetId = resolveTargetId(conversationId, type, currentUserId);
        return messageMapper.selectHistoryByConversation(conversationId, type.getCode(), currentUserId, targetId, cursorTime, limit);
    }

    /**
     * 增量同步
     */
    public List<Message> getSyncMessages(Long conversationId, ConversationType type,
                                          String cursorTime, int limit, Long currentUserId) {
        Long targetId = resolveTargetId(conversationId, type, currentUserId);
        return messageMapper.selectSyncByConversation(conversationId, type.getCode(), currentUserId, targetId, cursorTime, limit);
    }

    /** 从 conversation 记录中解析 targetId（私聊=对方userId，群聊=groupId） */
    private Long resolveTargetId(Long conversationId, ConversationType type, Long currentUserId) {
        if (conversationId != null && conversationId > 0) {
            Conversation conv = conversationMapper.selectById(conversationId);
            return conv != null ? conv.getTargetId() : null;
        }
        return null;
    }

    /**
     * 获取离线消息（seq > lastReadSeq）
     */
    public List<Message> getOfflineMessages(Long conversationId, Integer conversationType,
                                            Long lastSeq, int limit) {
        return lambdaQuery()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getConversationType, conversationType)
                .gt(lastSeq != null && lastSeq > 0, Message::getSeq, lastSeq)
                .orderByAsc(Message::getSeq)
                .last("limit " + Math.max(limit, 1))
                .list();
    }

    // ==================== 消息撤回 ====================

    /**
     * 撤回消息（2 分钟内），记录到 message_recall 表
     *
     * @return true=成功 false=超时或无权
     */
    @Transactional
    public boolean recallMessage(Long messageId, Long userId) {
        return recallMessage(messageId, userId, null);
    }

    @Transactional
    public boolean recallMessage(Long messageId, Long userId, String reason) {
        Message msg = getById(messageId);
        if (msg == null || !msg.getFromUserId().equals(userId)) return false;
        if (msg.getCreateTime().plusMinutes(2).isBefore(LocalDateTime.now())) {
            log.info("撤回超时: msgId={}, createTime={}", messageId, msg.getCreateTime());
            return false;
        }

        msg.setIsRecalled(1);
        msg.setStatus(MessageStatus.RECALLED.getCode());
        msg.setRecalledTime(LocalDateTime.now());
        updateById(msg);

        MessageRecall recall = new MessageRecall();
        recall.setMessageId(messageId);
        recall.setRecallBy(userId);
        recall.setReason(reason);
        recall.setRecallTime(LocalDateTime.now());
        messageRecallMapper.insert(recall);

        log.info("消息已撤回: msgId={}, userId={}, reason={}", messageId, userId, reason);
        return true;
    }

    // ==================== @提及 ====================

    /**
     * 解析消息内容中的 @{userId}，创建 mention 记录
     */
    void parseAndCreateMentions(Message msg) {
        String content = msg.getContent();
        if (content == null || !content.contains("@")) return;

        Matcher matcher = MENTION_PATTERN.matcher(content);
        List<Long> mentionedIds = new ArrayList<>();
        while (matcher.find()) {
            try { mentionedIds.add(Long.parseLong(matcher.group(1))); }
            catch (NumberFormatException ignored) {}
        }

        if (!mentionedIds.isEmpty()) {
            mentionService.createMentions(msg.getId(), msg.getFromUserId(), mentionedIds);
            log.info("解析@提及: msgId={}, mentioned={}", msg.getId(), mentionedIds);
        }
    }

    public List<?> getMentionMessages(Long userId, int page, int size) {
        return mentionService.getMentions(userId, page, size);
    }

    public long getUnreadMentionCount(Long userId) {
        return mentionService.getUnreadCount(userId);
    }

    // ==================== 内部 ====================

    /**
     * 查找或创建会话（发送者侧），返回 conversationId。
     * 同时为接收者也创建会话（同步，不等异步事件）
     */
    private Long resolveConversationId(Message msg) {
        int convType = msg.getConversationType() != null ? msg.getConversationType() : 0;
        Long targetId = msg.getToId();
        Long fromUserId = msg.getFromUserId();

        // 1. 发送者会话
        Long convId = getOrCreateConversation(fromUserId, convType, targetId);

        // 2. 单聊时同步创建接收者会话
        if (convType == 0) {
            getOrCreateConversation(targetId, convType, fromUserId);
        }
        return convId;
    }

    private Long getOrCreateConversation(Long userId, int convType, Long targetId) {
        Conversation exist = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getType, convType)
                        .eq(Conversation::getUserId, userId)
                        .eq(Conversation::getTargetId, targetId));
        if (exist != null) return exist.getId();

        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setType(convType);
        conv.setTargetId(targetId);
        conv.setUnreadCount(0);
        conversationMapper.insert(conv);
        log.info("自动创建会话: convId={}, userId={}, targetId={}", conv.getId(), userId, targetId);
        return conv.getId();
    }

    /** 更新双方会话的最后消息和未读数 */
    private void updateConversation(Message msg) {
        if (msg.getConversationType() == null) return;
        int type = msg.getConversationType();
        Long fromUserId = msg.getFromUserId();
        Long toId = msg.getToId();

        if (type == 0) {
            // 单聊：收方未读+1，发方仅更新最后消息不加未读
            conversationService.updateLastMessage(buildConv(toId, 0, fromUserId, msg), true);
            conversationService.updateLastMessage(buildConv(fromUserId, 0, toId, msg), false);
        } else {
            // 群聊：更新所有群成员的会话（除发送者不加未读）
            List<GroupMember> members = groupMemberMapper.selectList(
                    new LambdaQueryWrapper<GroupMember>()
                            .eq(GroupMember::getGroupId, toId));
            for (GroupMember member : members) {
                boolean isSender = member.getUserId().equals(fromUserId);
                conversationService.updateLastMessage(buildConv(member.getUserId(), 1, toId, msg), !isSender);
            }
        }
    }

    private Conversation buildConv(Long userId, int type, Long targetId, Message msg) {
        Conversation conv =
                new Conversation();
        conv.setUserId(userId);
        conv.setType(type);
        conv.setTargetId(targetId);
        conv.setLastMessageId(msg.getId());
        conv.setLastMessageSeq(msg.getSeq());
        return conv;
    }

    private String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }

    private String buildSeqKey(Message msg) {
        int type = msg.getConversationType() != null ? msg.getConversationType() : 0;
        Long convId = msg.getConversationId() != null ? msg.getConversationId() : msg.getToId();
        return "im:seq:" + type + ":" + convId;
    }
}
