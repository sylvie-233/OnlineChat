package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 黑名单实体
 */
@Data
@TableName("blocklist")
public class Blocklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long blockedUserId;
    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
