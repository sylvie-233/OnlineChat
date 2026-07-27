package com.sylvie233.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会话类型枚举
 */
@Getter
@AllArgsConstructor
public enum ConversationType {

    PRIVATE(0, "单聊"),
    GROUP(1, "群聊"),
    SYSTEM(2, "系统会话"),
    CHANNEL(3, "频道");

    private final int code;
    private final String desc;
}
