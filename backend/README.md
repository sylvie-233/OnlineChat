# OnlineChat 后端

基于 **Spring Boot 3.x + Netty + WebSocket + MyBatis-Plus + Redis + MySQL** 开发的在线即时通讯（IM）系统后端。

## 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [模块架构](#模块架构)
- [快速开始](#快速开始)
- [数据库设计](#数据库设计)
- [HTTP API](#http-api)
- [WebSocket 协议](#websocket-协议)
- [核心机制](#核心机制)
- [配置说明](#配置说明)
- [部署](#部署)

---

## 技术栈

| 组件 | 版本 | 用途 |
|---|---|---|
| Spring Boot | 3.3.6 | 应用框架 |
| Netty | 4.1.115 | 长连接网关（WebSocket） |
| MyBatis-Plus | 3.5.9 | ORM / 数据库访问 |
| MySQL | 8.x | 持久化存储 |
| Redis | 7.x | 缓存 / 在线状态 / seq / 离线消息 |
| Sa-Token | 1.39.0 | 认证鉴权（JWT） |
| Knife4j | 4.5.0 | API 文档（Swagger） |
| Hutool | 5.8.32 | 工具集（BCrypt 等） |
| Fastjson2 | 2.0.53 | JSON 序列化 |
| MinIO | - | 对象存储（文件/图片） |
| Maven | 3.x | 构建工具 |

## 项目结构

```
backend/
├── pom.xml                          # 父 POM（多模块管理）
├── onlinechat-common/               # 公共模块
│   └── src/main/java/com/sylvie233/common/
│       ├── enums/                   # 枚举类（5 个）
│       ├── exception/               # BizException
│       ├── model/
│       │   ├── dto/                 # 请求 DTO（8 个）
│       │   └── resp/                # Result / PageResult
│       └── util/                    # SnowflakeIdWorker（46-bit 轻量版，ID < 2^53）
├── onlinechat-repository/           # 数据访问层
│   └── src/main/java/com/sylvie233/repository/
│       ├── config/                  # MyBatis-Plus 自动填充
│       ├── entity/                  # 实体类（22 张表）
│       └── mapper/                  # Mapper 接口（21 个）
├── onlinechat-service/              # 业务逻辑层
│   └── src/main/java/com/sylvie233/service/
│       ├── cache/                   # RedisCacheService
│       ├── contact/                 # 好友/联系人
│       ├── conversation/            # 会话
│       ├── file/                    # 文件管理
│       ├── group/                   # 群组 + 群公告 + 入群申请
│       ├── message/                 # 消息 + 撤回 + Reaction + 收藏 + @提及 + 已读 + 归档
│       ├── notification/            # 通知
│       └── user/                    # 用户 + 设置 + 会话
├── onlinechat-connect/              # 长连接网关（Netty）
│   └── src/main/java/com/sylvie233/connect/
│       ├── handler/                 # WebSocketHandler
│       ├── protocol/                # ImPacket 协议
│       ├── router/                  # MessageRouter
│       ├── server/                  # NettyWebSocketServer
│       └── session/                 # ChannelSession / SessionManager
├── onlinechat-task/                 # 异步 + 定时任务
│   └── src/main/java/com/sylvie233/task/
│       ├── consumer/                # 消息队列消费者
│       └── scheduler/               # 会话清理 + 消息归档
└── onlinechat-server/               # Web 入口 + REST Controller
    └── src/main/java/com/sylvie233/server/
        ├── config/                  # CORS / MinIO / Redis / WebMVC
        ├── controller/              # 9 个 Controller
        └── interceptor/             # 登录拦截器
```

## 模块架构

```
┌─────────────────────────────────────────────┐
│                 Client 客户端                 │
│         (Web / iOS / Android / Desktop)       │
└──────────┬──────────────┬───────────────────┘
           │ HTTP         │ WebSocket
           ▼              ▼
┌──────────────────┐ ┌──────────────────────┐
│  onlinechat-     │ │  onlinechat-connect  │
│  server          │ │  (Netty :9090)       │
│  ───────────     │ │  ───────────────     │
│  9 Controllers   │ │  WebSocketHandler    │
│  AuthInterceptor │ │  MessageRouter       │
│  Knife4j文档     │ │  SessionManager      │
└────────┬─────────┘ └──────────┬───────────┘
         │                      │
         ▼                      ▼
┌─────────────────────────────────────────────┐
│              onlinechat-service              │
│  14 个 Service: 用户/好友/群组/消息/文件/通知  │
│  + RedisCacheService                         │
└────────┬────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│            onlinechat-repository             │
│  MyBatis-Plus: 22 Entity + 21 Mapper        │
└────────┬────────────────────────────────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌───────┐ ┌───────┐
│ MySQL │ │ Redis │
└───────┘ └───────┘

异步任务: onlinechat-task
  ├── MessageQueueConsumer  (Redis Stream → MySQL 批量入库)
  ├── SessionCleanTask      (@Scheduled 每5分钟)
  └── MessageArchiveTask    (@Scheduled 每天凌晨3点)
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+
- MinIO（可选，文件上传需要）

### 1. 初始化数据库

```bash
mysql -u root -p < scripts/init.sql
```

### 2. 配置

编辑 `onlinechat-server/src/main/resources/application.yml`，修改数据库/Redis/MinIO 连接信息。

最小配置（仅需 MySQL + Redis）：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/online_chat?...
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
```

### 3. 构建 & 运行

```bash
# 编译
mvn clean package -DskipTests

# 运行
java -jar onlinechat-server/target/onlinechat-server-1.0.0.jar
```

或使用项目根目录的脚本：
```bash
# Windows PowerShell
.\build.ps1 build
.\build.ps1 run
```

### 4. 访问

| 服务 | 地址 |
|---|---|
| HTTP API | `http://localhost:8080` |
| WebSocket | `ws://localhost:9090/ws` |
| Swagger 文档 | `http://localhost:8080/doc.html` |

## 数据库设计

### 核心 ER 关系

```
user ──< contact >── user        (好友关系，双向)
user ──< friend_request >── user (好友申请)
user ──< blocklist >── user      (黑名单)
user ──< user_session            (多端登录)
user ──< user_setting            (个人设置，1:1)
user ──< conversation            (聊天列表)
user ──< message                 (发送消息)
user ──< notification            (系统通知)

user ──< group_member >── group_info       (群成员)
group_info ──< group_request >── user      (入群申请)
group_info ──< group_announcement          (群公告)
group_announcement ──< group_announcement_read >── user

message ──< message_read >── user          (已读回执)
message ──< message_recall                 (撤回记录)
message ──< message_reaction >── user      (表情回应)
message ──< message_mention >── user       (@提及)
message ──< message_bookmark >── user      (消息收藏)

message ──< message_archive                (冷数据归档)
```

### 表清单（22 张）

| 模块 | 表名 | 说明 | 关键字段 |
|---|---|---|---|
| 用户 | `user` | 用户主表 | username, password(bcrypt), online_status, status(封禁/禁言) |
| 用户 | `user_setting` | 个人设置 | 通知/声音/验证策略/主题/语言/聊天背景 |
| 用户 | `user_session` | 多端登录会话 | token, refresh_token, device_type, device_id |
| 好友 | `contact_group` | 好友分组 | group_name, sort_order |
| 好友 | `contact` | 好友关系 | user_id, contact_user_id, remark, is_starred, source |
| 好友 | `friend_request` | 好友申请 | from_user_id, to_user_id, verify_message, status |
| 黑名单 | `blocklist` | 黑名单 | user_id, blocked_user_id, reason |
| 会话 | `conversation` | 聊天列表 | user_id, type, target_id, unread_count, is_pinned, is_muted, draft |
| 消息 | `message` | 消息主表 | seq, conversation_type_id, msg_type, status, client_msg_id, extra |
| 消息 | `message_read` | 已读回执 | message_id, user_id, read_time |
| 消息 | `message_recall` | 撤回记录 | message_id, recall_by, reason |
| 消息 | `message_reaction` | 表情回应 | message_id, user_id, emoji |
| 消息 | `message_mention` | @提及 | message_id, from_user_id, to_user_id, is_read |
| 消息 | `message_bookmark` | 消息收藏 | user_id, message_id, tag |
| 消息 | `message_archive` | 消息归档 | 与 message 同结构，按月分区 |
| 群组 | `group_info` | 群组信息 | group_name, owner_id, join_type, is_muted_all |
| 群组 | `group_member` | 群成员 | group_id, user_id, role, nickname_in_group |
| 群组 | `group_request` | 入群申请/邀请 | group_id, from_user_id, to_user_id, type, status |
| 群组 | `group_announcement` | 群公告 | group_id, publisher_id, title, content, is_pinned |
| 群组 | `group_announcement_read` | 公告已读 | announcement_id, user_id, read_time |
| 文件 | `file_upload` | 文件上传记录 | file_name, file_url, file_type, thumbnail_url, storage_type |
| 通知 | `notification` | 系统通知 | user_id, type, title, content, related_id, is_read |

## HTTP API

> 所有接口前缀 `/api`。除登录/注册外，需在 Header 携带 `Authorization` Token。

### 认证 — `/api/auth`

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| `POST` | `/api/auth/register` | 注册（BCrypt 加密） | 否 |
| `POST` | `/api/auth/login` | 登录，返回 Token + userId + nickname | 否 |
| `POST` | `/api/auth/logout` | 登出 | 是 |
| `POST` | `/api/auth/refresh` | 刷新 Token | 是 |

### 用户 — `/api/user`

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/user/{id}` | 获取用户信息（脱敏） |
| `GET` | `/api/user/search?username=xx` | 搜索用户 |
| `PUT` | `/api/user/profile` | 更新个人资料 |
| `PUT` | `/api/user/online-status` | 更新在线状态（0离线/1在线/2隐身/3忙碌） |
| `GET` | `/api/user/settings` | 获取个人设置 |
| `PUT` | `/api/user/settings` | 更新个人设置 |
| `GET` | `/api/user/sessions` | 多端会话列表 |
| `POST` | `/api/user/sessions/{id}/kick` | 强制登出指定设备 |

### 联系人 — `/api/contact`

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/contact/groups` | 好友分组列表 |
| `POST` | `/api/contact/groups` | 创建分组 |
| `PUT` | `/api/contact/groups/{id}` | 重命名分组 |
| `DELETE` | `/api/contact/groups/{id}` | 删除分组 |
| `GET` | `/api/contact/list` | 好友列表 |
| `DELETE` | `/api/contact/{contactUserId}` | 删除好友 |
| `PUT` | `/api/contact/remark` | 修改好友备注 |
| `PUT` | `/api/contact/star` | 星标/取消星标 |
| `PUT` | `/api/contact/move-group` | 移动好友到其他分组 |
| `POST` | `/api/contact/request` | 发送好友申请 |
| `GET` | `/api/contact/requests` | 收到的好友申请列表 |
| `PUT` | `/api/contact/request/{id}` | 处理好友申请（同意/拒绝） |
| `POST` | `/api/contact/block/{userId}` | 拉黑用户 |
| `DELETE` | `/api/contact/block/{userId}` | 取消拉黑 |
| `GET` | `/api/contact/blocks` | 黑名单列表 |

### 会话 — `/api/conversation`

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/conversation/list` | 聊天列表 |
| `PUT` | `/api/conversation/{id}/pin` | 置顶/取消置顶 |
| `PUT` | `/api/conversation/{id}/mute` | 免打扰/取消免打扰 |
| `PUT` | `/api/conversation/{id}/clear-unread` | 清除未读数 |
| `PUT` | `/api/conversation/{id}/draft` | 保存草稿 |
| `PUT` | `/api/conversation/{id}/hide` | 隐藏会话 |
| `DELETE` | `/api/conversation/{id}` | 删除会话 |

### 消息 — `/api/message`

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/message/send` | HTTP 发送消息（降级通道） |
| `GET` | `/api/message/latest` | 拉取会话最新消息 |
| `GET` | `/api/message/history` | 历史消息翻页 |
| `GET` | `/api/message/sync` | 增量同步（seq 游标） |
| `PUT` | `/api/message/{id}/recall` | 撤回消息（2 分钟内） |
| `POST` | `/api/message/{id}/retry` | 重发失败消息 |
| `PUT` | `/api/message/{id}/read` | HTTP 标记已读 |
| `GET` | `/api/message/{id}/read-count` | 获取已读人数 |
| `GET` | `/api/message/{id}/read-users` | 获取已读用户列表 |
| `POST` | `/api/message/{id}/reaction` | 添加表情 Reaction |
| `DELETE` | `/api/message/{id}/reaction` | 移除表情 Reaction |
| `GET` | `/api/message/{id}/reactions` | 获取 Reaction 列表（按 emoji 聚合） |
| `POST` | `/api/message/{id}/bookmark` | 收藏消息 |
| `DELETE` | `/api/message/bookmark/{id}` | 取消收藏 |
| `GET` | `/api/message/bookmarks` | 收藏列表（支持 tag 筛选） |
| `GET` | `/api/message/bookmark-tags` | 收藏标签列表 |
| `GET` | `/api/message/mentions` | @我的消息 |
| `GET` | `/api/message/mentions/unread-count` | 未读 @提及数 |

### 群组 — `/api/group`

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/group` | 创建群 |
| `GET` | `/api/group/{id}` | 获取群信息 |
| `PUT` | `/api/group/{id}/settings` | 更新群设置 |
| `DELETE` | `/api/group/{id}` | 解散群（仅群主） |
| `GET` | `/api/group/{id}/members` | 群成员列表 |
| `POST` | `/api/group/{id}/join` | 加入群 |
| `POST` | `/api/group/{id}/invite/{userId}` | 邀请入群 |
| `DELETE` | `/api/group/{id}/member/{userId}` | 踢出成员 |
| `PUT` | `/api/group/{id}/member/{userId}/role` | 设置/取消管理员 |
| `PUT` | `/api/group/{id}/member/{userId}/nickname` | 设置群内昵称 |
| `PUT` | `/api/group/{id}/member/settings` | 成员免打扰/置顶 |
| `POST` | `/api/group/{id}/apply` | 申请加入群 |
| `GET` | `/api/group/{id}/requests` | 群申请列表 |
| `PUT` | `/api/group/request/{id}` | 处理入群申请 |
| `GET` | `/api/group/invitations` | 收到的入群邀请 |
| `POST` | `/api/group/{id}/announcement` | 发布群公告 |
| `PUT` | `/api/group/announcement/{id}` | 编辑群公告 |
| `DELETE` | `/api/group/announcement/{id}` | 删除群公告 |
| `GET` | `/api/group/{id}/announcements` | 群公告列表 |
| `PUT` | `/api/group/announcement/{id}/read` | 标记公告已读 |
| `GET` | `/api/group/message/{id}/read-stats` | 群消息已读/未读统计 |
| `GET` | `/api/group/{id}/mentions` | 群内 @我的消息 |

### 通知 — `/api/notification`

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/notification/list` | 通知列表 |
| `GET` | `/api/notification/unread-count` | 未读通知数 |
| `PUT` | `/api/notification/{id}/read` | 标记单条已读 |
| `PUT` | `/api/notification/read-all` | 全部标记已读 |

### 文件 — `/api/file`

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/file/upload` | 上传文件（图片自动生成缩略图） |
| `GET` | `/api/file/{id}/url` | 获取预签名 URL（7 天有效） |
| `DELETE` | `/api/file/{id}` | 删除文件 |

### 管理后台 — `/api/admin`

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/admin/users` | 用户列表（分页、搜索） |
| `PUT` | `/api/admin/users/{id}/ban` | 封禁/解封用户 |
| `PUT` | `/api/admin/users/{id}/mute` | 禁言/取消禁言 |
| `GET` | `/api/admin/stats` | 系统统计（用户/群/在线数） |
| `GET` | `/api/admin/users/online` | 在线用户列表 |

## WebSocket 协议

### 连接信息

```
地址: ws://localhost:9090/ws
协议: JSON 文本帧
编码: UTF-8
心跳: 客户端 120s 发送一次，服务端 IdleStateHandler 300s 读空闲检测
```

### ImPacket 消息格式

```json
{
  "cmd": 100,
  "seq": 12345,
  "timestamp": 1722098000000,
  "body": { ... }
}
```

### 命令码完整列表

| 命令码 | 常量 | 方向 | 说明 |
|---|---|---|---|
| `0` | `CMD_HEARTBEAT` | C→S | 心跳请求 |
| `1` | `CMD_HEARTBEAT_ACK` | S→C | 心跳响应 |
| `10` | `CMD_AUTH` | C→S | 登录认证 |
| `11` | `CMD_AUTH_ACK` | S→C | 认证结果 |
| `100` | `CMD_PRIVATE_MSG` | C→S | 发送单聊消息 |
| `101` | `CMD_PRIVATE_MSG_ACK` | S→C | 单聊消息 ACK（含 msgId） |
| `200` | `CMD_GROUP_MSG` | C→S | 发送群聊消息 |
| `201` | `CMD_GROUP_MSG_ACK` | S→C | 群聊消息 ACK |
| `300` | `CMD_READ_NOTIFY` | C↔S | 已读通知 + 多端同步 |
| `301` | `CMD_RECALL_NOTIFY` | C→S | 撤回消息通知 |
| `400` | `CMD_ONLINE_NOTIFY` | C→S | 在线状态变更 |
| `500` | `CMD_PUSH_MSG` | S→C | 服务端推送新消息 |
| `501` | `CMD_PUSH_NOTIFY` | S→C | 系统通知推送 |
| `600` | `CMD_TYPING` | C→S | 正在输入通知 |
| `601` | `CMD_TYPING_ACK` | S→C | 正在输入转发 |
| `700` | `CMD_FORWARD_MSG` | C→S | 转发消息 |
| `-1` | `CMD_ERROR` | S→C | 错误响应 |

### 典型消息流程

```
发送单聊消息:
  Client A                              Server                           Client B
     │── CMD_AUTH {userId,token} ──────►│                                  │
     │◄─ CMD_AUTH_ACK ──────────────────│                                  │
     │                                   │◄── CMD_AUTH ────────────────────│
     │                                   │── CMD_AUTH_ACK ────────────────►│
     │                                   │                                  │
     │── CMD_PRIVATE_MSG {toUserId,msg}►│                                  │
     │                                   │── 落库 + 推送 ─────────────────►│
     │◄─ CMD_PRIVATE_MSG_ACK {msgId} ───│                                  │
     │                                   │◄── CMD_READ_NOTIFY ─────────────│
     │                                   │── 已读同步到 A 其他设备 ────────►│
```

## 核心机制

### 消息状态机

```
发送中(SENDING=0) → 已发送(SENT=1) → 已送达(DELIVERED=2) → 已读(READ=4)
                                              ↓
                                         发送失败(FAILED=5) → 重发 → SENT
                                              ↓
                                         已撤回(RECALLED=6)
```

- 客户端发送消息 → 服务端落库 → 状态变为 `SENT`
- 对方在线且推送成功 → 状态变为 `DELIVERED`
- 对方上报已读 → 状态变为 `READ`
- 2 分钟内可撤回 → 状态变为 `RECALLED`

### 消息幂等

基于 `client_msg_id`（客户端生成唯一 ID）实现去重：
- 发送时检查 `client_msg_id` 是否已存在
- 存在则直接返回已有消息，不重复落库

### seq 序列号

会话级别的递增序列号（Redis INCR）：
- Key: `im:seq:{type}:{conversationId}`
- 每条消息分配唯一 seq
- 消息排序优先使用 `send_time`（实际发送时间），seq 仅作同毫秒内的副排序

### 离线消息

```
用户离线 → 消息存入 Redis List (im:offline:{userId})
用户上线 → auth 成功后自动拉取并推送全部离线消息
Redis TTL: 7 天
```

### 消息限流

基于 Redis 滑动窗口实现：
- Key: `im:ratelimit:{userId}`
- 默认限制: 10 条/秒
- 超限返回错误提示

### 在线状态管理

```
Redis:    im:online:{userId}  → serverNode  (TTL 30min)
          im:channel:{userId} → channelId
MySQL:    user.online_status  → 0离线/1在线/2隐身/3忙碌
SessionManager: ConcurrentHashMap 维护 Channel ↔ User 映射（支持多端）
清理:     退出登录 / WS 断开 → 立即删除 Redis key + 更新 MySQL
```

### 多端同步

- 同一用户可在多设备同时在线（Web/iOS/Android/Desktop）
- 已读状态自动同步到所有设备
- 同 deviceId 新登录会踢掉旧连接
- 通过 `GET /api/user/sessions` 查看所有活跃会话

### 冷热分离（消息归档）

```
message 表（热数据）  ←→  message_archive 表（冷数据）
每天凌晨 3 点自动归档超过 3 个月的旧消息
每次批量 1000 条
```

### 好友验证策略

| friendVerifyType | 行为 |
|---|---|
| 0（允许所有人） | 直接建立好友关系，无需申请 |
| 1（需要验证） | 正常申请→同意/拒绝流程 |
| 2（拒绝所有人） | 直接拒绝 |

## 配置说明

### application.yml 核心配置

```yaml
# Netty WebSocket
im:
  websocket:
    port: 9090              # WebSocket 端口
    path: /ws               # WebSocket 路径
  message:
    recall-window-minutes: 2  # 消息撤回窗口

# Sa-Token
sa-token:
  token-name: Authorization
  timeout: 2592000          # Token 有效期（秒），默认 30 天
  is-concurrent: true       # 允许并发登录

# MinIO
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minio123456
  bucket: onlinechat
```

### 环境变量

| 变量 | 默认值 | 说明 |
|---|---|---|
| `REDIS_HOST` | `localhost` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | `redis123456` | Redis 密码 |

## 部署

### JAR 包部署

```bash
mvn clean package -DskipTests
java -jar onlinechat-server/target/onlinechat-server-1.0.0.jar \
  --spring.datasource.url=jdbc:mysql://your-db:3306/online_chat \
  --spring.data.redis.host=your-redis \
  --minio.endpoint=http://your-minio:9000
```

### Docker Compose

项目根目录 `scripts/docker-compose.yml` 已提供 MySQL + Redis + MinIO 的编排：

```bash
docker-compose -f scripts/docker-compose.yml up -d
```

### 多实例部署

多实例时需注意：
- 雪花算法已精简为单实例版本，多实例需引入 worker-id 区分
- Redis 用于跨节点 Channel 映射（`im:channel:{userId}`）
- SessionManager 是内存级，跨节点消息路由走 Redis 查找目标节点

---

> 项目地址: `d:/Project/OnlineChat/backend`  
> 前端: Vue3 + TypeScript + Vite + Ant-Design-Vue (client + admin 双端)  
> Swagger: `http://localhost:8080/doc.html`
