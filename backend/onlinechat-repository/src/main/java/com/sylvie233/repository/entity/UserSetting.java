package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户设置实体
 */
@Data
@TableName("user_setting")
public class UserSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer msgNotifyEnabled;
    private Integer soundEnabled;
    private Integer vibrateEnabled;
    private Integer showDetailEnabled;
    private Integer friendVerifyType;
    private Integer groupInviteVerify;
    private String theme;
    private String language;
    private String fontSize;
    private String chatBgUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
