package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 群申请/邀请记录实体
 */
@Data
@TableName("group_request")
public class GroupRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;
    private Long fromUserId;
    private Long toUserId;
    private Integer type;
    private String verifyMessage;
    private Integer status;
    private LocalDateTime handledTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
