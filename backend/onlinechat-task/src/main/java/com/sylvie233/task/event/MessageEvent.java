package com.sylvie233.task.event;

import com.sylvie233.repository.entity.Message;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 消息事件 — 消息落库后发布，异步推送 & 通知
 */
@Getter
public class MessageEvent extends ApplicationEvent {

    private final Message message;

    public MessageEvent(Object source, Message message) {
        super(source);
        this.message = message;
    }
}
