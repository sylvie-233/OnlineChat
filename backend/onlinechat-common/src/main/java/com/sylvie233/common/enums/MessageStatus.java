package com.sylvie233.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息状态枚举
 */
@Getter
@AllArgsConstructor
public enum MessageStatus {

    SENDING(0, "发送中"),
    SENT(1, "已发送"),
    DELIVERED(2, "已送达"),
    PARTIAL_READ(3, "部分已读"),
    ALL_READ(4, "全部已读"),
    FAILED(5, "发送失败"),
    RECALLED(6, "已撤回");

    private final int code;
    private final String desc;
}
