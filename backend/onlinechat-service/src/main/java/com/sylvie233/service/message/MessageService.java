package com.sylvie233.service.message;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.common.enums.ConversationType;
import com.sylvie233.common.enums.MessageStatus;
import com.sylvie233.repository.entity.Message;
import com.sylvie233.repository.entity.MessageRecall;
import com.sylvie233.repository.mapper.MessageMapper;
import com.sylvie233.repository.mapper.MessageRecallMapper;
import com.sylvie233.service.cache.RedisCacheService;
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

        // 2. seq 自增
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
     * 拉取会话最新 N 条消息
     */
    public List<Message> getLatestMessages(Long conversationId, ConversationType type, int limit) {
        return messageMapper.selectLatestByConversation(conversationId, type.getCode(), limit);
    }

    /**
     * 向前翻页拉取历史消息（seq < cursorSeq）
     */
    public List<Message> getHistoryMessages(Long conversationId, ConversationType type,
                                             Long cursorSeq, int limit) {
        return messageMapper.selectHistoryByConversation(conversationId, type.getCode(), cursorSeq, limit);
    }

    /**
     * 增量同步 — 获取 sinceSeq 之后的新消息（seq > sinceSeq）
     */
    public List<Message> getSyncMessages(Long conversationId, ConversationType type,
                                          Long sinceSeq, int limit) {
        return messageMapper.selectSyncByConversation(conversationId, type.getCode(), sinceSeq, limit);
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

    private String buildSeqKey(Message msg) {
        int type = msg.getConversationType() != null ? msg.getConversationType() : 0;
        Long convId = msg.getConversationId() != null ? msg.getConversationId() : msg.getToId();
        return "im:seq:" + type + ":" + convId;
    }
}
