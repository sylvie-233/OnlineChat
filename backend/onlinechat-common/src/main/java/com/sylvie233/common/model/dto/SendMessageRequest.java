package com.sylvie233.common.model.dto;

import lombok.Data;

/**
 * 发送消息请求
 */
@Data
public class SendMessageRequest {
    /** 会话类型: 0=单聊 1=群聊 */
    private Integer conversationType;
    /** 会话ID */
    private Long conversationId;
    /** 接收者ID（用户ID 或 群ID） */
    private Long toId;
    /** 消息类型 */
    private Integer msgType;
    /** 消息内容 */
    private String content;
    /** 扩展字段 JSON */
    private String extra;
    /** 客户端消息ID（幂等去重） */
    private String clientMsgId;
    /** 引用消息ID */
    private Long replyToMsgId;
    /** @提及的用户ID列表 */
    private java.util.List<Long> mentionUserIds;
}
