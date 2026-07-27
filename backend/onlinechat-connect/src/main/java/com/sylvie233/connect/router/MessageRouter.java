package com.sylvie233.connect.router;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sylvie233.connect.protocol.ImPacket;
import com.sylvie233.connect.session.ChannelSession;
import com.sylvie233.connect.session.SessionManager;
import com.sylvie233.repository.entity.GroupMember;
import com.sylvie233.repository.entity.Message;
import com.sylvie233.repository.mapper.GroupMemberMapper;
import com.sylvie233.service.cache.RedisCacheService;
import com.sylvie233.service.message.MessageService;
import com.sylvie233.service.message.MessageReadService;
import com.sylvie233.service.user.UserService;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 消息路由器 — 消息分发/群广播/已读/撤回/在线状态/正在输入/转发
 * 含离线消息推送 & 发消息限流
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRouter {

    private final SessionManager sessionManager;
    private final RedisCacheService redisCacheService;
    private final MessageService messageService;
    private final MessageReadService messageReadService;
    private final UserService userService;
    private final GroupMemberMapper groupMemberMapper;

    private static final int RATE_LIMIT_PER_SEC = 10;

    // ==================== 认证 ====================

    public void handleAuth(Channel channel, ChannelSession session, ImPacket packet) {
        JSONObject body = (JSONObject) packet.getBody();
        String token = body.getString("token");
        String deviceType = body.getString("deviceType");
        String deviceId = body.getString("deviceId");
        // 优先用 token 反查真实 userId，防止客户端传错 ID
        Long userId = body.getLong("userId");
        if (token != null && !token.isEmpty()) {
            try {
                Object loginId = cn.dev33.satoken.stp.StpUtil.getLoginIdByToken(token);
                if (loginId != null) {
                    long tokenUserId = Long.parseLong(loginId.toString());
                    if (userId == null || !userId.equals(tokenUserId)) {
                        log.warn("客户端传入 userId={}, token 真实 userId={}, 以 token 为准", userId, tokenUserId);
                        userId = tokenUserId;
                    }
                }
            } catch (Exception e) { /* token 解析失败，使用 body userId */ }
        }

        if (userId == null) {
            channel.writeAndFlush(new TextWebSocketFrame(
                    JSON.toJSONString(ImPacket.error(packet.getSeq(), "Token 无效"))));
            return;
        }

        sessionManager.bindUser(session.channelId(), userId, deviceType, deviceId);
        redisCacheService.bindChannel(userId, session.channelId());
        redisCacheService.setOnline(userId, "node1");
        userService.online(userId);

        ImPacket ack = new ImPacket(ImPacket.CMD_AUTH_ACK, packet.getSeq(),
                System.currentTimeMillis(), "认证成功");
        channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(ack)));
        log.info("用户 {} 认证成功, device={}", userId, deviceType);

        // 推送离线消息
        pushOfflineMessages(channel, userId);
    }

    // ==================== 消息路由 ====================

    public void route(Channel channel, ChannelSession session, ImPacket packet) {
        JSONObject body = (JSONObject) packet.getBody();

        // 限流检查
        if (!redisCacheService.checkRateLimit(session.getUserId(), RATE_LIMIT_PER_SEC)) {
            channel.writeAndFlush(new TextWebSocketFrame(
                    JSON.toJSONString(ImPacket.error(packet.getSeq(), "发送过于频繁，请稍后再试"))));
            return;
        }

        if (packet.getCmd() == ImPacket.CMD_FORWARD_MSG) {
            handleForward(session, packet);
            return;
        }

        Message msg = buildMessage(session, body);

        if (packet.getCmd() == ImPacket.CMD_PRIVATE_MSG) {
            Long toUserId = body.getLong("toUserId");
            msg.setToId(toUserId);
            msg.setConversationType(0);
            routeToUser(channel, session.getUserId(), toUserId, msg, packet.getSeq());
        } else if (packet.getCmd() == ImPacket.CMD_GROUP_MSG) {
            Long groupId = body.getLong("groupId");
            msg.setToId(groupId);
            msg.setConversationType(1);
            routeToGroup(channel, groupId, msg, packet.getSeq());
        }
    }

    private Message buildMessage(ChannelSession session, JSONObject body) {
        Message msg = new Message();
        msg.setFromUserId(session.getUserId());
        msg.setMsgType(body.getInteger("msgType"));
        msg.setContent(body.getString("content"));
        msg.setClientMsgId(body.getString("clientMsgId"));
        msg.setExtra(body.getString("extra"));
        msg.setReplyToMsgId(body.getLong("replyToMsgId"));
        Long convId = body.getLong("conversationId");
        msg.setConversationId(convId != null ? convId : 0L);

        // 携带发送者昵称（用于前端显示，不依赖 userId 比对）
        com.sylvie233.repository.entity.User sender = userService.getById(session.getUserId());
        if (sender != null && msg.getExtra() == null) {
            msg.setExtra("{\"fromNickname\":\"" + (sender.getNickname() != null ? sender.getNickname() : "") + "\"}");
        }
        return msg;
    }

    private void routeToUser(Channel channel, Long senderId, Long toUserId, Message msg, long clientSeq) {
        messageService.sendMessage(msg);

        if (redisCacheService.isOnline(toUserId)) {
            // 在线：直接推送 + 标记已送达
            pushToUser(toUserId, msg);
            messageService.updateStatus(msg.getId(),
                    com.sylvie233.common.enums.MessageStatus.DELIVERED);
        } else {
            // 离线：存入 Redis 离线消息队列
            redisCacheService.storeOfflineMessage(toUserId, msg);
        }

        ack(channel, ImPacket.CMD_PRIVATE_MSG_ACK, clientSeq, msg);
    }

    private void routeToGroup(Channel channel, Long groupId, Message msg, long clientSeq) {
        messageService.sendMessage(msg);

        List<GroupMember> members = groupMemberMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId));

        for (GroupMember member : members) {
            if (member.getUserId().equals(msg.getFromUserId())) continue;
            if (redisCacheService.isOnline(member.getUserId())) {
                pushToUser(member.getUserId(), msg);
            } else {
                redisCacheService.storeOfflineMessage(member.getUserId(), msg);
            }
        }

        ack(channel, ImPacket.CMD_GROUP_MSG_ACK, clientSeq, msg);
        log.info("群聊广播: groupId={}, members={}", groupId, members.size());
    }

    // ==================== 转发 ====================

    private void handleForward(ChannelSession session, ImPacket packet) {
        JSONObject body = (JSONObject) packet.getBody();
        Long originalMsgId = body.getLong("originalMsgId");
        Long targetId = body.getLong("targetId");
        Integer targetType = body.getInteger("targetType");

        Message original = messageService.getById(originalMsgId);
        if (original == null) {
            session.getChannel().writeAndFlush(new TextWebSocketFrame(
                    JSON.toJSONString(ImPacket.error(packet.getSeq(), "原消息不存在"))));
            return;
        }

        Message forward = new Message();
        forward.setFromUserId(session.getUserId());
        forward.setMsgType(original.getMsgType());
        forward.setContent(original.getContent());
        forward.setExtra("{\"forwarded\":true,\"originalMsgId\":" + originalMsgId + "}");
        forward.setConversationType(targetType);
        forward.setToId(targetId);

        messageService.sendMessage(forward);
        if (targetType == 0) pushToUser(targetId, forward);
        ack(session.getChannel(), ImPacket.CMD_PRIVATE_MSG_ACK, packet.getSeq(), forward);
        log.info("消息转发: msgId={} -> targetId={}", originalMsgId, targetId);
    }

    // ==================== 离线消息 ====================

    private void pushOfflineMessages(Channel channel, Long userId) {
        List<Object> offlineMessages = redisCacheService.fetchOfflineMessages(userId);
        if (offlineMessages.isEmpty()) return;

        log.info("推送离线消息: userId={}, count={}", userId, offlineMessages.size());
        for (Object msg : offlineMessages) {
            ImPacket push = ImPacket.push(msg);
            channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(push)));
        }
    }

    // ==================== 已读 / 撤回 / 在线状态 / 正在输入 ====================

    public void handleReadNotify(ChannelSession session, ImPacket packet) {
        JSONObject body = (JSONObject) packet.getBody();
        Long messageId = parseLong(body.get("messageId"));
        if (messageId != null) {
            try {
                messageReadService.markAsRead(messageId, session.getUserId());
                syncReadToOtherDevices(session, messageId);
            } catch (Exception e) {
                log.warn("已读标记失败（消息可能不存在）: messageId={}", messageId);
            }
        }
    }

    /**
     * 多端已读同步 — 推送已读状态到同一用户的其他设备
     */
    private void syncReadToOtherDevices(ChannelSession currentSession, Long messageId) {
        Set<Channel> allChannels = sessionManager.getUserChannels(currentSession.getUserId());
        ImPacket readSync = new ImPacket(ImPacket.CMD_READ_NOTIFY, 0,
                System.currentTimeMillis(), messageId);
        String json = JSON.toJSONString(readSync);
        for (Channel ch : allChannels) {
            if (ch.isActive() && !ch.id().asShortText().equals(currentSession.channelId())) {
                ch.writeAndFlush(new TextWebSocketFrame(json));
            }
        }
    }

    public void handleRecallNotify(ChannelSession session, ImPacket packet) {
        JSONObject body = (JSONObject) packet.getBody();
        Long messageId = body.getLong("messageId");
        String reason = body.getString("reason");
        boolean success = messageService.recallMessage(messageId, session.getUserId(), reason);
        if (!success) {
            session.getChannel().writeAndFlush(new TextWebSocketFrame(
                    JSON.toJSONString(ImPacket.error(packet.getSeq(), "撤回失败：超过2分钟或无权操作"))));
        }
    }

    public void handleOnlineStatusNotify(ChannelSession session, ImPacket packet) {
        JSONObject body = (JSONObject) packet.getBody();
        Integer status = body.getInteger("status");
        if (status != null) {
            userService.updateOnlineStatus(session.getUserId(), status);
        }
    }

    public void handleTyping(ChannelSession session, ImPacket packet) {
        JSONObject body = (JSONObject) packet.getBody();
        Long toUserId = body.getLong("toUserId");
        Long groupId = body.getLong("groupId");

        ImPacket typingNotify = new ImPacket(ImPacket.CMD_TYPING_ACK, 0,
                System.currentTimeMillis(), body);

        if (toUserId != null) {
            pushToUserRaw(toUserId, typingNotify);
        } else if (groupId != null) {
            List<GroupMember> members = groupMemberMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GroupMember>()
                            .eq(GroupMember::getGroupId, groupId));
            for (GroupMember member : members) {
                if (!member.getUserId().equals(session.getUserId())) {
                    pushToUserRaw(member.getUserId(), typingNotify);
                }
            }
        }
    }

    // ==================== 工具方法 ====================

    private void pushToUser(Long userId, Message msg) {
        ImPacket push = ImPacket.push(msg);
        pushToUserRaw(userId, push);
    }

    private void pushToUserRaw(Long userId, ImPacket packet) {
        String json = JSON.toJSONString(packet);
        Set<Channel> channels = sessionManager.getUserChannels(userId);
        for (Channel ch : channels) {
            if (ch.isActive()) ch.writeAndFlush(new TextWebSocketFrame(json));
        }
    }

    private Long parseLong(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) { try { return Long.parseLong((String) v); } catch (NumberFormatException e) {} }
        return null;
    }

    private void ack(Channel channel, int cmd, long seq, Object data) {
        channel.writeAndFlush(new TextWebSocketFrame(
                JSON.toJSONString(new ImPacket(cmd, seq, System.currentTimeMillis(), data))));
    }
}
