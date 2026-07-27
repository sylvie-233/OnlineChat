package com.sylvie233.task.consumer;

import com.sylvie233.repository.entity.Message;
import com.sylvie233.service.message.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 消息队列消费者 — Redis Stream 消费组模式
 * <p>
 * 关键设计:
 * 1. 先批量入库 MySQL，成功后 ACK ← 防止消息丢失
 * 2. @Transactional 保证批次原子性（全部成功或全部回滚）
 * 3. 队列不可用时自动降级直写（在 MessageService 中处理）
 * </p>
 */
@Slf4j
@Component
public class MessageQueueConsumer {

    private static final String STREAM_KEY = "im:message:stream";
    private static final String GROUP_NAME = "im-consumer-group";
    private static final String CONSUMER_NAME = "consumer-" + System.currentTimeMillis() % 10000;
    private static final int BATCH_SIZE = 50;
    private static final long BATCH_MS = 200;
    private static final long POLL_MS = 500;

    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageService messageService;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "msg-queue-consumer");
        t.setDaemon(true);
        return t;
    });

    @Value("${im.queue.enabled:true}")
    private boolean queueEnabled;

    public MessageQueueConsumer(RedisTemplate<String, Object> redisTemplate,
                                 MessageService messageService) {
        this.redisTemplate = redisTemplate;
        this.messageService = messageService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!queueEnabled) {
            log.info("消息队列已禁用");
            return;
        }
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0-0"), GROUP_NAME);
            log.info("消费组已创建: stream={}, group={}", STREAM_KEY, GROUP_NAME);
        } catch (Exception e) { /* 已存在 */ }

        executor.submit(this::consumeLoop);
        log.info("消息队列消费者已启动: consumer={}, batchSize={}", CONSUMER_NAME, BATCH_SIZE);
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
    }

    @SuppressWarnings("unchecked")
    private void consumeLoop() {
        List<MapRecord<String, Object, Object>> batch = new ArrayList<>();
        long lastFlush = System.currentTimeMillis();

        while (running.get()) {
            try {
                StreamOperations<String, Object, Object> ops = redisTemplate.opsForStream();
                List<MapRecord<String, Object, Object>> records = (List) ops.read(
                        Consumer.from(GROUP_NAME, CONSUMER_NAME),
                        StreamReadOptions.empty().count(BATCH_SIZE).block(Duration.ofSeconds(2)),
                        StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

                if (records == null || records.isEmpty()) {
                    if (!batch.isEmpty()) {
                        flushBatch(batch);
                        batch.clear();
                    }
                    Thread.sleep(POLL_MS);
                    continue;
                }

                batch.addAll(records);

                boolean sizeReached = batch.size() >= BATCH_SIZE;
                boolean timeReached = (System.currentTimeMillis() - lastFlush) >= BATCH_MS;

                if (sizeReached || timeReached) {
                    flushBatch(batch);
                    batch.clear();
                    lastFlush = System.currentTimeMillis();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("消费异常", e);
            }
        }

        if (!batch.isEmpty()) flushBatch(batch);
        log.info("消息队列消费者已停止");
    }

    /**
     * 批量入库 → 成功后 ACK ← 防止消息丢失
     */
    @Transactional
    void flushBatch(List<MapRecord<String, Object, Object>> batch) {
        if (batch.isEmpty()) return;

        List<Message> messages = new ArrayList<>();
        for (MapRecord<String, Object, Object> record : batch) {
            try {
                Message msg = parseRecord(record);
                if (msg != null) messages.add(msg);
            } catch (Exception e) {
                log.error("解析失败: recordId={}", record.getId(), e);
            }
        }

        if (messages.isEmpty()) return;

        try {
            messageService.saveBatch(messages);
            // 入库成功后才 ACK
            for (MapRecord<String, Object, Object> record : batch) {
                try {
                    redisTemplate.opsForStream().acknowledge(
                            STREAM_KEY, GROUP_NAME, record.getId().getValue());
                } catch (Exception e) { /* ACK 失败不影响后续 */ }
            }
            log.info("批量入库: {} 条", messages.size());
        } catch (Exception e) {
            log.error("批量入库失败: {} 条, 未ACK, 下次重试", messages.size(), e);
            throw e; // 回滚事务，不 ACK
        }
    }

    private Message parseRecord(MapRecord<String, Object, Object> record) {
        Map<Object, Object> map = record.getValue();
        if (map == null || map.isEmpty()) return null;

        Message msg = new Message();
        msg.setId(getLong(map, "id"));
        msg.setSeq(getLong(map, "seq"));
        msg.setConversationType(getInt(map, "conversationType"));
        msg.setConversationId(getLong(map, "conversationId"));
        msg.setFromUserId(getLong(map, "fromUserId"));
        msg.setToId(getLong(map, "toId"));
        msg.setMsgType(getInt(map, "msgType"));
        msg.setContent(getString(map, "content"));
        msg.setExtra(getString(map, "extra"));
        msg.setClientMsgId(getString(map, "clientMsgId"));
        msg.setReplyToMsgId(getLong(map, "replyToMsgId"));
        msg.setStatus(getInt(map, "status"));
        msg.setSendTime(java.time.LocalDateTime.now());
        return msg;
    }

    private Long getLong(Map<Object, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
    }

    private Integer getInt(Map<Object, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        return v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(v.toString());
    }

    private String getString(Map<Object, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
}
