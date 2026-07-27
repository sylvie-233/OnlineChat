package com.sylvie233.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型枚举
 */
@Getter
@AllArgsConstructor
public enum MsgType {

    TEXT(0, "文本"),
    IMAGE(1, "图片"),
    VOICE(2, "语音"),
    VIDEO(3, "视频"),
    FILE(4, "文件"),
    LOCATION(5, "位置"),
    LINK(6, "链接分享"),
    SYSTEM(7, "系统通知"),
    CARD(8, "自定义卡片");

    private final int code;
    private final String desc;

    public static MsgType of(int code) {
        for (MsgType t : values()) {
            if (t.code == code) return t;
        }
        return TEXT;
    }
}
