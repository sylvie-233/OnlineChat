package com.sylvie233.service.message;

import com.sylvie233.common.util.SnowflakeIdWorker;
import com.sylvie233.repository.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息队列生产者 — 将消息发布到 Redis Stream
 * <p>
 * 由 MessageService 内部调用，透明切换直写/队列模式：
 * 1. 预生成 Snowflake 消息 ID
 * 2. 发布到 Redis Stream "im:message:stream"
 * 3. MessageQueueConsumer 异步批量拉取 → MySQL
 * </p>
 */
@Slf4j
@Component
public class MessageQueueProducer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SnowflakeIdWorker idWorker;

    private static final String STREAM_KEY = "im:message:stream";
    private static final long STREAM_MAX_LEN = 100_000;

    @Value("${im.queue.enabled:true}")
    private boolean queueEnabled;

    public MessageQueueProducer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.idWorker = new SnowflakeIdWorker();
    }

    /**
     * 发布消息到 Redis Stream
     *
     * @param msg 消息实体（自动预生成 Snowflake ID）
     * @return RecordId 流记录 ID，失败/禁用时返回 null
     */
    public String publish(Message msg) {
        if (!queueEnabled) return null;

        try {
            if (msg.getId() == null) {
                msg.setId(idWorker.nextId());
            }

            Map<String, Object> fields = toMap(msg);
            RecordId recordId = redisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .in(STREAM_KEY)
                            .ofMap(fields));

            log.debug("消息入队: msgId={}, recordId={}", msg.getId(), recordId);
            return recordId != null ? recordId.getValue() : null;
        } catch (Exception e) {
            log.error("消息入队失败, 降级直写: msgId={}", msg.getId(), e);
            return null;
        }
    }

    public boolean isEnabled() { return queueEnabled; }

    public long queueSize() {
        try {
            Long len = redisTemplate.opsForStream().size(STREAM_KEY);
            return len != null ? len : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public void trim() {
        try { redisTemplate.opsForStream().trim(STREAM_KEY, STREAM_MAX_LEN); }
        catch (Exception e) { log.warn("Stream 裁剪失败: {}", e.getMessage()); }
    }

    private Map<String, Object> toMap(Message msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", msg.getId());
        map.put("seq", msg.getSeq());
        map.put("conversationType", msg.getConversationType());
        map.put("conversationId", msg.getConversationId());
        map.put("fromUserId", msg.getFromUserId());
        map.put("toId", msg.getToId());
        map.put("msgType", msg.getMsgType());
        map.put("content", msg.getContent());
        map.put("extra", msg.getExtra());
        map.put("clientMsgId", msg.getClientMsgId());
        map.put("replyToMsgId", msg.getReplyToMsgId());
        map.put("status", msg.getStatus());
        return map;
    }
}
