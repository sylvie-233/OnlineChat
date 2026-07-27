package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户登录会话实体
 */
@Data
@TableName("user_session")
public class UserSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String token;
    private String refreshToken;
    private String deviceType;
    private String deviceName;
    private String deviceId;
    private String clientIp;
    private LocalDateTime lastActiveTime;
    private LocalDateTime expireTime;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
