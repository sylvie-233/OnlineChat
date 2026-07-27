package com.sylvie233.connect.protocol;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * IM 自定义消息协议包
 * <pre>
 * 协议格式（JSON 文本）:
 * {
 *   "cmd": 1,           // 命令类型
 *   "seq": 123,          // 客户端序列号（用于 ACK）
 *   "timestamp": 123456,
 *   "body": { ... }      // 消息体（各命令不同）
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImPacket {

    /** 命令码 */
    private int cmd;
    /** 客户端序列号（请求-响应匹配） */
    private long seq;
    /** 发送时间戳 */
    private long timestamp;
    /** JSON 消息体 */
    private Object body;

    // ========== 命令码常量 ==========

    /** 心跳请求 / 响应 */
    public static final int CMD_HEARTBEAT = 0;
    public static final int CMD_HEARTBEAT_ACK = 1;

    /** 登录认证 */
    public static final int CMD_AUTH = 10;
    public static final int CMD_AUTH_ACK = 11;

    /** 单聊消息 */
    public static final int CMD_PRIVATE_MSG = 100;
    public static final int CMD_PRIVATE_MSG_ACK = 101;

    /** 群聊消息 */
    public static final int CMD_GROUP_MSG = 200;
    public static final int CMD_GROUP_MSG_ACK = 201;

    /** 消息已读通知 */
    public static final int CMD_READ_NOTIFY = 300;
    /** 消息撤回通知 */
    public static final int CMD_RECALL_NOTIFY = 301;

    /** 用户上线/离线通知 */
    public static final int CMD_ONLINE_NOTIFY = 400;

    /** 服务端推送（新消息到达） */
    public static final int CMD_PUSH_MSG = 500;
    /** 系统通知推送 */
    public static final int CMD_PUSH_NOTIFY = 501;

    /** 正在输入 */
    public static final int CMD_TYPING = 600;
    public static final int CMD_TYPING_ACK = 601;

    /** 转发消息 */
    public static final int CMD_FORWARD_MSG = 700;

    /** 错误响应 */
    public static final int CMD_ERROR = -1;

    // ========== 工厂方法 ==========

    public static ImPacket heartbeat() {
        return new ImPacket(CMD_HEARTBEAT, 0, System.currentTimeMillis(), null);
    }

    public static ImPacket heartbeatAck() {
        return new ImPacket(CMD_HEARTBEAT_ACK, 0, System.currentTimeMillis(), null);
    }

    public static ImPacket push(Object msg) {
        return new ImPacket(CMD_PUSH_MSG, 0, System.currentTimeMillis(), msg);
    }

    public static ImPacket notify(Object data) {
        return new ImPacket(CMD_PUSH_NOTIFY, 0, System.currentTimeMillis(), data);
    }

    public static ImPacket error(long seq, String msg) {
        return new ImPacket(CMD_ERROR, seq, System.currentTimeMillis(), msg);
    }
}
