package com.sylvie233.task.event;

import com.sylvie233.repository.entity.Message;
import com.sylvie233.service.cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 消息事件监听器 — 事务提交后异步执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventListener {

    private final RedisCacheService redisCacheService;

    /**
     * 处理新消息事件（事务提交后触发）
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageEvent(MessageEvent event) {
        Message msg = event.getMessage();
        log.info("异步处理消息事件: msgId={}, type={}", msg.getId(), msg.getMsgType());

        // 1. 更新会话（最后消息 + 未读计数）
        // conversationService.updateLastMessage(...);

        // 2. 发送推送通知
        // notificationService.push(...);

        // 3. 离线消息处理
        // offlineMessageService.storeIfOffline(...);
    }
}
