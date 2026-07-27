package com.sylvie233.task.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 消息队列消费者（Redis Stream / RocketMQ）
 * <p>异步消息落库，削峰填谷</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageQueueConsumer {

    // TODO: 接入 Redis Stream 消费组，异步批量写入 message 表

    /**
     * 消费消息落库
     */
    // @StreamListener("im:message:stream")
    public void onMessage(String msgJson) {
        log.debug("消费消息: {}", msgJson);
        // 批量 insert message
    }
}
