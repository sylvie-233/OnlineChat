package com.sylvie233.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户在线状态
 */
@Getter
@AllArgsConstructor
public enum OnlineStatus {

    OFFLINE(0, "离线"),
    ONLINE(1, "在线"),
    INVISIBLE(2, "隐身"),
    BUSY(3, "忙碌");

    private final int code;
    private final String desc;
}
