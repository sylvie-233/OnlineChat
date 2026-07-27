package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息表实体
 */
@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.ASSIGN_ID)  // 雪花算法
    private Long id;

    private Long seq;
    private Integer conversationType;
    private Long conversationId;
    private Long fromUserId;
    private Long toId;
    private Integer msgType;
    private String content;
    private String extra;         // JSON 扩展字段
    private Long replyToMsgId;
    private Integer status;
    private Integer isDeleted;
    private Integer isRecalled;
    private LocalDateTime recalledTime;
    private String clientMsgId;   // 幂等去重
    private LocalDateTime sendTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
