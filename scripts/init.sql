-- ============================================================================
-- OnlineChat IM System — MySQL Init Schema
-- 技术栈: Spring Boot 4.x + MyBatis-Plus + Netty + WebSocket + Redis
-- ============================================================================

-- MySQL 客户端/连接编码设置
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS online_chat
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
USE online_chat;

-- ============================================================================
-- 1. 用户模块 (User Module)
-- ============================================================================

-- 用户主表
CREATE TABLE `user` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
    `username`          VARCHAR(32)     NOT NULL                 COMMENT '用户名',
    `password`          VARCHAR(128)    NOT NULL                 COMMENT '密码(bcrypt)',
    `nickname`          VARCHAR(64)     NOT NULL                 COMMENT '昵称',
    `avatar`            VARCHAR(512)    DEFAULT ''               COMMENT '头像URL',
    `phone`             VARCHAR(20)     DEFAULT NULL             COMMENT '手机号',
    `email`             VARCHAR(128)    DEFAULT NULL             COMMENT '邮箱',
    `gender`            TINYINT         DEFAULT 0                COMMENT '性别: 0=未知 1=男 2=女',
    `bio`               VARCHAR(512)    DEFAULT ''               COMMENT '个人简介',
    `birthday`          DATE            DEFAULT NULL             COMMENT '生日',
    `region`            VARCHAR(128)    DEFAULT ''               COMMENT '地区',
    `status`            TINYINT         DEFAULT 0                COMMENT '状态: 0=正常 1=禁言 2=封禁',
    `last_login_time`   DATETIME        DEFAULT NULL             COMMENT '最后登录时间',
    `last_login_ip`     VARCHAR(64)     DEFAULT ''               COMMENT '最后登录IP',
    `online_status`     TINYINT         DEFAULT 0                COMMENT '在线状态: 0=离线 1=在线 2=隐身 3=忙碌',
    `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`),
    KEY `idx_online_status` (`online_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户设置表
CREATE TABLE `user_setting` (
    `id`                    BIGINT      NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    `user_id`               BIGINT      NOT NULL                 COMMENT '用户ID',
    `msg_notify_enabled`    TINYINT     NOT NULL DEFAULT 1       COMMENT '消息通知开关: 0=关 1=开',
    `sound_enabled`         TINYINT     NOT NULL DEFAULT 1       COMMENT '声音开关',
    `vibrate_enabled`       TINYINT     NOT NULL DEFAULT 1       COMMENT '振动开关',
    `show_detail_enabled`   TINYINT     NOT NULL DEFAULT 1       COMMENT '通知显示详情开关',
    `friend_verify_type`    TINYINT     NOT NULL DEFAULT 0       COMMENT '好友验证方式: 0=允许所有人 1=需要验证 2=拒绝所有人',
    `group_invite_verify`   TINYINT     NOT NULL DEFAULT 0       COMMENT '群邀请验证: 0=允许所有人 1=需要验证 2=拒绝',
    `theme`                 VARCHAR(32) DEFAULT 'default'        COMMENT '主题设置',
    `language`              VARCHAR(16) DEFAULT 'zh_CN'          COMMENT '语言',
    `font_size`             VARCHAR(16) DEFAULT 'medium'         COMMENT '字体大小',
    `chat_bg_url`           VARCHAR(512) DEFAULT ''              COMMENT '聊天背景图URL',
    `create_time`           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_user_setting_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户设置表';

-- ============================================================================
-- 2. 好友/联系人模块 (Contact Module)
-- ============================================================================

-- 好友分组表
CREATE TABLE `contact_group` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '分组ID',
    `user_id`       BIGINT       NOT NULL                 COMMENT '所属用户ID',
    `group_name`    VARCHAR(64)  NOT NULL DEFAULT '默认分组' COMMENT '分组名称',
    `sort_order`    INT          NOT NULL DEFAULT 0       COMMENT '排序序号',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_contact_group_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系人分组表';

-- 好友关系表
CREATE TABLE `contact` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    `user_id`           BIGINT       NOT NULL                 COMMENT '用户ID',
    `contact_user_id`   BIGINT       NOT NULL                 COMMENT '联系人用户ID',
    `group_id`          BIGINT       DEFAULT NULL             COMMENT '所属分组ID',
    `remark`            VARCHAR(64)  DEFAULT ''               COMMENT '备注名',
    `is_starred`        TINYINT      NOT NULL DEFAULT 0       COMMENT '是否星标好友: 0=否 1=是',
    `source`            VARCHAR(32)  DEFAULT ''               COMMENT '添加来源',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_contact` (`user_id`, `contact_user_id`),
    KEY `idx_contact_user` (`contact_user_id`),
    KEY `idx_group_id` (`group_id`),
    CONSTRAINT `fk_contact_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_contact_contact_user` FOREIGN KEY (`contact_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- 好友申请记录表
CREATE TABLE `friend_request` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    `from_user_id`      BIGINT        NOT NULL                 COMMENT '发起方用户ID',
    `to_user_id`        BIGINT        NOT NULL                 COMMENT '接收方用户ID',
    `verify_message`    VARCHAR(256)  DEFAULT ''               COMMENT '验证消息',
    `remark`            VARCHAR(64)   DEFAULT ''               COMMENT '发起方对对方的备注',
    `status`            TINYINT       NOT NULL DEFAULT 0       COMMENT '状态: 0=待处理 1=已同意 2=已拒绝 3=已过期',
    `handled_time`      DATETIME      DEFAULT NULL             COMMENT '处理时间',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_from_user` (`from_user_id`),
    KEY `idx_to_user` (`to_user_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_fr_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_fr_to_user` FOREIGN KEY (`to_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请记录表';

-- ============================================================================
-- 3. 黑名单模块 (Blocklist Module)
-- ============================================================================

CREATE TABLE `blocklist` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    `user_id`           BIGINT       NOT NULL                 COMMENT '用户ID(拉黑方)',
    `blocked_user_id`   BIGINT       NOT NULL                 COMMENT '被拉黑的用户ID',
    `reason`            VARCHAR(256) DEFAULT ''               COMMENT '拉黑原因',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_blocked` (`user_id`, `blocked_user_id`),
    CONSTRAINT `fk_blk_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_blk_blocked_user` FOREIGN KEY (`blocked_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户黑名单表';

-- ============================================================================
-- 4. 会话模块 (Conversation Module)
-- ============================================================================

-- 会话类型: 0=单聊 1=群聊 2=系统会话 3=频道
-- 会话表 — 相当于用户视角的"聊天列表"中的每一项
CREATE TABLE `conversation` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '会话ID',
    `user_id`           BIGINT        NOT NULL                 COMMENT '用户ID',
    `type`              TINYINT       NOT NULL                 COMMENT '会话类型: 0=单聊 1=群聊 2=系统 3=频道',
    `target_id`         BIGINT        NOT NULL                 COMMENT '目标ID(对方用户ID 或 群ID)',
    `last_message_id`   BIGINT        DEFAULT NULL             COMMENT '最后一条消息ID',
    `last_message_seq`  BIGINT        DEFAULT 0                COMMENT '最后消息序列号(用于增量同步)',
    `unread_count`      INT           NOT NULL DEFAULT 0       COMMENT '未读消息数',
    `is_pinned`         TINYINT       NOT NULL DEFAULT 0       COMMENT '是否置顶: 0=否 1=是',
    `is_muted`          TINYINT       NOT NULL DEFAULT 0       COMMENT '是否免打扰: 0=否 1=是',
    `is_hidden`         TINYINT       NOT NULL DEFAULT 0       COMMENT '是否隐藏: 0=否 1=是',
    `draft`             VARCHAR(4096) DEFAULT ''               COMMENT '草稿内容',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_type_target` (`user_id`, `type`, `target_id`),
    KEY `idx_user_pinned` (`user_id`, `is_pinned`),
    KEY `idx_last_msg` (`last_message_id`),
    CONSTRAINT `fk_conv_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表(用户视角的聊天列表)';

-- ============================================================================
-- 5. 消息模块 (Message Module) — 核心
-- ============================================================================

-- 消息类型: 0=文本 1=图片 2=语音 3=视频 4=文件 5=位置 6=链接 7=系统通知 8=自定义卡片
-- 消息状态: 0=发送中 1=已发送 2=已送达 3=已读 4=发送失败 5=已撤回
CREATE TABLE `message` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '消息ID(雪花算法,全局唯一)',
    `seq`               BIGINT          NOT NULL                 COMMENT '会话级别消息序列号(递增)',
    `conversation_type` TINYINT         NOT NULL                 COMMENT '会话类型: 0=单聊 1=群聊 2=系统',
    `conversation_id`   BIGINT          NOT NULL DEFAULT 0        COMMENT '会话ID(关联conversation.id)',
    `from_user_id`      BIGINT          NOT NULL                 COMMENT '发送者用户ID',
    `to_id`             BIGINT          NOT NULL                 COMMENT '接收者ID(用户ID或群ID)',
    `msg_type`          TINYINT         NOT NULL DEFAULT 0       COMMENT '消息类型: 0=文本 1=图片 2=语音 3=视频 4=文件 5=位置 6=链接 7=系统 8=卡片',
    `content`           TEXT            DEFAULT NULL             COMMENT '消息内容(文本消息正文,其他类型存JSON)',
    `extra`             JSON            DEFAULT NULL             COMMENT '扩展字段(JSON): 图片尺寸/语音时长/文件大小等',
    `reply_to_msg_id`   BIGINT          DEFAULT NULL             COMMENT '被引用的消息ID(回复消息)',
    `status`            TINYINT         NOT NULL DEFAULT 1       COMMENT '消息状态: 0=发送中 1=已发送 2=已送达 3=部分已读(群) 4=全部已读 5=发送失败 6=已撤回',
    `is_deleted`        TINYINT         NOT NULL DEFAULT 0       COMMENT '是否删除(仅发送方): 0=否 1=已删除',
    `is_recalled`       TINYINT         NOT NULL DEFAULT 0       COMMENT '是否撤回: 0=否 1=已撤回',
    `recalled_time`     DATETIME        DEFAULT NULL             COMMENT '撤回时间',
    `client_msg_id`     VARCHAR(64)     DEFAULT NULL             COMMENT '客户端消息ID(幂等去重)',
    `send_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间(客户端时间)',
    `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '服务端创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_conv_seq` (`conversation_id`, `conversation_type`, `seq`),
    KEY `idx_from_user` (`from_user_id`),
    KEY `idx_to_id_type` (`to_id`, `conversation_type`),
    KEY `idx_send_time` (`send_time`),
    KEY `idx_client_msg_id` (`client_msg_id`),
    KEY `idx_reply_to` (`reply_to_msg_id`),
    KEY `idx_recalled` (`is_recalled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 消息已读回执表(用于多设备同步 & 群聊已读统计)
CREATE TABLE `message_read` (
    `id`                BIGINT      NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    `message_id`        BIGINT      NOT NULL                 COMMENT '消息ID',
    `user_id`           BIGINT      NOT NULL                 COMMENT '已读用户ID',
    `read_time`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_user` (`message_id`, `user_id`),
    KEY `idx_user_read` (`user_id`, `read_time`),
    CONSTRAINT `fk_mr_message` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mr_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息已读回执表';

-- ============================================================================
-- 6. 群组模块 (Group Module)
-- ============================================================================

-- 群组信息表
CREATE TABLE `group_info` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '群ID',
    `group_name`        VARCHAR(128)  NOT NULL                 COMMENT '群名称',
    `avatar`            VARCHAR(512)  DEFAULT ''               COMMENT '群头像URL',
    `owner_id`          BIGINT        NOT NULL                 COMMENT '群主用户ID',
    `announcement`      TEXT          DEFAULT NULL             COMMENT '群公告',
    `description`       VARCHAR(1024) DEFAULT ''               COMMENT '群简介',
    `max_members`       INT           NOT NULL DEFAULT 200     COMMENT '最大成员数',
    `member_count`      INT           NOT NULL DEFAULT 0       COMMENT '当前成员数(冗余,快速查询)',
    `join_type`         TINYINT       NOT NULL DEFAULT 0       COMMENT '加群方式: 0=自由加入 1=需要验证 2=禁止加入 3=邀请制',
    `is_muted_all`      TINYINT       NOT NULL DEFAULT 0       COMMENT '全员禁言: 0=否 1=是',
    `status`            TINYINT       NOT NULL DEFAULT 0       COMMENT '群状态: 0=正常 1=已解散',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_owner` (`owner_id`),
    KEY `idx_group_name` (`group_name`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组信息表';

-- 群成员表
-- 成员角色: 0=普通成员 1=管理员 2=群主
CREATE TABLE `group_member` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    `group_id`          BIGINT       NOT NULL                 COMMENT '群ID',
    `user_id`           BIGINT       NOT NULL                 COMMENT '用户ID',
    `role`              TINYINT      NOT NULL DEFAULT 0       COMMENT '角色: 0=普通成员 1=管理员 2=群主',
    `nickname_in_group` VARCHAR(64)  DEFAULT ''               COMMENT '群内昵称',
    `unread_count`      INT          NOT NULL DEFAULT 0       COMMENT '群内未读消息数',
    `is_muted`          TINYINT      NOT NULL DEFAULT 0       COMMENT '是否免打扰: 0=否 1=是',
    `is_pinned`         TINYINT      NOT NULL DEFAULT 0       COMMENT '是否置顶: 0=否 1=是',
    `last_read_seq`     BIGINT       NOT NULL DEFAULT 0       COMMENT '最后读取的消息seq',
    `join_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    KEY `idx_user_groups` (`user_id`),
    KEY `idx_role` (`group_id`, `role`),
    CONSTRAINT `fk_gm_group` FOREIGN KEY (`group_id`) REFERENCES `group_info` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_gm_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- 群申请记录表
CREATE TABLE `group_request` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    `group_id`          BIGINT        NOT NULL                 COMMENT '群ID',
    `from_user_id`      BIGINT        NOT NULL                 COMMENT '发起方用户ID',
    `to_user_id`        BIGINT        DEFAULT NULL             COMMENT '处理方用户ID(群主/管理员)',
    `type`              TINYINT       NOT NULL                 COMMENT '类型: 0=用户申请入群 1=邀请用户入群',
    `verify_message`    VARCHAR(256)  DEFAULT ''               COMMENT '验证消息',
    `status`            TINYINT       NOT NULL DEFAULT 0       COMMENT '状态: 0=待处理 1=已同意 2=已拒绝 3=已过期',
    `handled_time`      DATETIME      DEFAULT NULL             COMMENT '处理时间',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_group` (`group_id`),
    KEY `idx_from_user` (`from_user_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_gr_group` FOREIGN KEY (`group_id`) REFERENCES `group_info` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_gr_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群申请/邀请记录表';

-- ============================================================================
-- 7. 消息撤回记录模块 (Message Recall Module)
-- ============================================================================

CREATE TABLE `message_recall` (
    `id`                BIGINT      NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    `message_id`        BIGINT      NOT NULL                 COMMENT '被撤回的消息ID',
    `recall_by`         BIGINT      NOT NULL                 COMMENT '撤回操作人ID',
    `recall_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '撤回时间',
    `reason`            VARCHAR(256) DEFAULT ''              COMMENT '撤回原因',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_id` (`message_id`),
    KEY `idx_recall_by` (`recall_by`),
    CONSTRAINT `fk_mrecall_msg` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息撤回记录表';

-- ============================================================================
-- 8. @提及模块 (Mention Module)
-- ============================================================================

CREATE TABLE `message_mention` (
    `id`                BIGINT      NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    `message_id`        BIGINT      NOT NULL                 COMMENT '消息ID',
    `from_user_id`      BIGINT      NOT NULL                 COMMENT '@发起者用户ID',
    `to_user_id`        BIGINT      NOT NULL                 COMMENT '被@用户ID',
    `is_read`           TINYINT     NOT NULL DEFAULT 0       COMMENT '是否已读: 0=未读 1=已读',
    `create_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_message` (`message_id`),
    KEY `idx_to_user_read` (`to_user_id`, `is_read`),
    CONSTRAINT `fk_mm_msg` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mm_to_user` FOREIGN KEY (`to_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息@提及表';

-- ============================================================================
-- 9. 消息表情回应模块 (Message Reaction Module)
-- ============================================================================

CREATE TABLE `message_reaction` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    `message_id`        BIGINT       NOT NULL                 COMMENT '消息ID',
    `user_id`           BIGINT       NOT NULL                 COMMENT '用户ID',
    `emoji`             VARCHAR(64)  NOT NULL                 COMMENT '表情符号/表情ID',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_msg_emoji` (`message_id`, `emoji`),
    KEY `idx_user` (`user_id`),
    UNIQUE KEY `uk_msg_user_emoji` (`message_id`, `user_id`, `emoji`),
    CONSTRAINT `fk_mreact_msg` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mreact_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表情回应表';

-- ============================================================================
-- 10. 文件/媒体模块 (File / Media Module)
-- ============================================================================

CREATE TABLE `file_upload` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '文件ID',
    `user_id`           BIGINT        NOT NULL                 COMMENT '上传者用户ID',
    `file_name`         VARCHAR(256)  NOT NULL                 COMMENT '原始文件名',
    `file_url`          VARCHAR(1024) NOT NULL                 COMMENT '文件访问URL',
    `file_type`         VARCHAR(32)   NOT NULL                 COMMENT '文件类型: image/audio/video/file',
    `mime_type`         VARCHAR(128)  DEFAULT ''               COMMENT 'MIME类型',
    `file_size`         BIGINT        NOT NULL DEFAULT 0       COMMENT '文件大小(字节)',
    `duration`          INT           DEFAULT 0                COMMENT '时长(秒,音视频)',
    `width`             INT           DEFAULT 0                COMMENT '宽度(px,图片/视频)',
    `height`            INT           DEFAULT 0                COMMENT '高度(px,图片/视频)',
    `thumbnail_url`     VARCHAR(1024) DEFAULT ''               COMMENT '缩略图URL',
    `storage_type`      VARCHAR(32)   NOT NULL DEFAULT 'local' COMMENT '存储类型: local/oss/minio/cos',
    `status`            TINYINT       NOT NULL DEFAULT 1       COMMENT '状态: 0=上传中 1=上传成功 2=上传失败',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_file_type` (`file_type`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_fu_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传记录表';

-- ============================================================================
-- 11. 消息收藏/书签模块 (Bookmark Module)
-- ============================================================================

CREATE TABLE `message_bookmark` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    `user_id`           BIGINT       NOT NULL                 COMMENT '用户ID',
    `message_id`        BIGINT       NOT NULL                 COMMENT '被收藏的消息ID',
    `tag`               VARCHAR(64)  DEFAULT ''               COMMENT '收藏标签',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_msg` (`user_id`, `message_id`),
    KEY `idx_user_tag` (`user_id`, `tag`),
    CONSTRAINT `fk_mb_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mb_msg` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息收藏表';

-- ============================================================================
-- 12. 通知模块 (Notification Module)
-- ============================================================================

-- 通知类型: 0=系统通知 1=好友申请 2=群邀请 3=@提醒 4=会话消息
CREATE TABLE `notification` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '通知ID',
    `user_id`           BIGINT        NOT NULL                 COMMENT '接收者用户ID',
    `type`              TINYINT       NOT NULL                 COMMENT '通知类型: 0=系统 1=好友申请 2=群邀请 3=@提醒 4=会话消息',
    `title`             VARCHAR(256)  NOT NULL                 COMMENT '通知标题',
    `content`           VARCHAR(1024) DEFAULT ''               COMMENT '通知内容',
    `related_id`        BIGINT        DEFAULT NULL             COMMENT '关联业务ID(如friend_request.id)',
    `is_read`           TINYINT       NOT NULL DEFAULT 0       COMMENT '是否已读: 0=未读 1=已读',
    `read_time`         DATETIME      DEFAULT NULL             COMMENT '阅读时间',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`, `create_time`),
    KEY `idx_type` (`type`),
    CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ============================================================================
-- 13. 用户登录会话模块 (Session Module)
-- ============================================================================

CREATE TABLE `user_session` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '会话ID',
    `user_id`           BIGINT        NOT NULL                 COMMENT '用户ID',
    `token`             VARCHAR(512)  NOT NULL                 COMMENT '登录Token(JWT)',
    `refresh_token`     VARCHAR(512)  DEFAULT ''               COMMENT '刷新Token',
    `device_type`       VARCHAR(32)   NOT NULL DEFAULT 'unknown' COMMENT '设备类型: web/ios/android/desktop',
    `device_name`       VARCHAR(128)  DEFAULT ''               COMMENT '设备名称',
    `device_id`         VARCHAR(128)  DEFAULT ''               COMMENT '设备唯一标识',
    `client_ip`         VARCHAR(64)   DEFAULT ''               COMMENT '客户端IP',
    `last_active_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    `expire_time`       DATETIME      NOT NULL                 COMMENT '过期时间',
    `status`            TINYINT       NOT NULL DEFAULT 1       COMMENT '状态: 0=已登出 1=活跃 2=已过期',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token`(255)),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_expire_time` (`expire_time`),
    CONSTRAINT `fk_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录会话表';

-- ============================================================================
-- 14. 群公告模块 (Group Announcement Module)
-- ============================================================================

CREATE TABLE `group_announcement` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    `group_id`          BIGINT        NOT NULL                 COMMENT '群ID',
    `publisher_id`      BIGINT        NOT NULL                 COMMENT '发布者用户ID',
    `title`             VARCHAR(256)  NOT NULL                 COMMENT '公告标题',
    `content`           TEXT          NOT NULL                 COMMENT '公告内容',
    `is_pinned`         TINYINT       NOT NULL DEFAULT 0       COMMENT '是否置顶: 0=否 1=是',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_group` (`group_id`, `create_time`),
    CONSTRAINT `fk_ga_group` FOREIGN KEY (`group_id`) REFERENCES `group_info` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ga_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群公告表';

-- 群公告已读确认表
CREATE TABLE `group_announcement_read` (
    `id`                BIGINT      NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    `announcement_id`   BIGINT      NOT NULL                 COMMENT '公告ID',
    `user_id`           BIGINT      NOT NULL                 COMMENT '已读用户ID',
    `read_time`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ann_user` (`announcement_id`, `user_id`),
    CONSTRAINT `fk_gar_ann` FOREIGN KEY (`announcement_id`) REFERENCES `group_announcement` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_gar_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群公告已读确认表';

-- ============================================================================
-- 15. 数据归档模块 (Archive Module — 消息归档/冷热分离)
-- ============================================================================

CREATE TABLE `message_archive` (
    `id`                BIGINT          NOT NULL                COMMENT '消息ID',
    `seq`               BIGINT          NOT NULL                COMMENT '会话级别消息序列号',
    `conversation_type` TINYINT         NOT NULL                COMMENT '会话类型',
    `conversation_id`   BIGINT          NOT NULL                COMMENT '会话ID',
    `from_user_id`      BIGINT          NOT NULL                COMMENT '发送者用户ID',
    `to_id`             BIGINT          NOT NULL                COMMENT '接收者ID',
    `msg_type`          TINYINT         NOT NULL DEFAULT 0      COMMENT '消息类型',
    `content`           TEXT            DEFAULT NULL            COMMENT '消息内容',
    `extra`             JSON            DEFAULT NULL            COMMENT '扩展字段',
    `send_time`         DATETIME        NOT NULL                COMMENT '发送时间',
    `create_time`       DATETIME        NOT NULL                COMMENT '创建时间',
    `archive_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    PRIMARY KEY (`id`),
    KEY `idx_conv_time` (`conversation_id`, `conversation_type`, `send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息归档表(冷数据)';

-- ============================================================================
-- 索引优化建议 (根据业务场景按需创建)
-- ============================================================================

-- 消息表按月分区建议(MySQL 8.0+):
-- ALTER TABLE message PARTITION BY RANGE (TO_DAYS(create_time)) (
--     PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-02-01')),
--     PARTITION p202402 VALUES LESS THAN (TO_DAYS('2024-03-01')),
--     ...
-- );

-- ============================================================================
-- 初始化系统用户(可选)
-- ============================================================================

-- 系统消息机器人
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `bio`, `status`)
VALUES (0, 'system_bot', '', '系统通知', '系统消息机器人', 0)
ON DUPLICATE KEY UPDATE `nickname` = '系统通知';
