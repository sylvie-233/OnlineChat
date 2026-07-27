package com.sylvie233.connect.router;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sylvie233.connect.protocol.ImPacket;
import com.sylvie233.connect.session.ChannelSession;
import com.sylvie233.connect.session.SessionManager;
import com.sylvie233.service.cache.RedisCacheService;
import com.sylvie233.service.message.MessageService;
import com.sylvie233.repository.entity.Message;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Set;

/**
 * 消息路由器
 * <p>负责消息分发：单聊转发、群聊广播、跨节点路由</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRouter {

    private final SessionManager sessionManager;
    private final RedisCacheService redisCacheService;
    private final MessageService messageService;

    /**
     * 认证处理
     */
    public void handleAuth(Channel channel, ChannelSession session, ImPacket packet) {
        JSONObject body = (JSONObject) packet.getBody();
        String token = body.getString("token");
        String deviceType = body.getString("deviceType");
        String deviceId = body.getString("deviceId");

        // TODO: 解析 JWT Token，获取 userId
        // Long userId = JwtUtil.getUserId(token);
        Long userId = body.getLong("userId"); // 临时：从 body 取

        if (userId == null) {
            channel.writeAndFlush(new TextWebSocketFrame(
                    JSON.toJSONString(ImPacket.error(packet.getSeq(), "Token 无效"))));
            return;
        }

        sessionManager.bindUser(session.channelId(), userId, deviceType, deviceId);
        redisCacheService.bindChannel(userId, session.channelId());

        // 返回认证成功
        ImPacket ack = new ImPacket(ImPacket.CMD_AUTH_ACK, packet.getSeq(),
                System.currentTimeMillis(), "认证成功");
        channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(ack)));
    }

    /**
     * 消息路由分发
     */
    public void route(Channel channel, ChannelSession session, ImPacket packet) {
        JSONObject body = (JSONObject) packet.getBody();

        // 构建消息实体
        Message msg = new Message();
        msg.setFromUserId(session.getUserId());
        msg.setMsgType(body.getInteger("msgType"));
        msg.setContent(body.getString("content"));
        msg.setClientMsgId(body.getString("clientMsgId"));
        msg.setExtra(body.getString("extra"));

        if (packet.getCmd() == ImPacket.CMD_PRIVATE_MSG) {
            Long toUserId = body.getLong("toUserId");
            msg.setToId(toUserId);
            msg.setConversationType(0); // 单聊
            routeToUser(toUserId, msg, packet.getSeq());
        } else if (packet.getCmd() == ImPacket.CMD_GROUP_MSG) {
            Long groupId = body.getLong("groupId");
            msg.setToId(groupId);
            msg.setConversationType(1); // 群聊
            routeToGroup(groupId, msg, packet.getSeq());
        }
    }

    /**
     * 单聊消息路由
     */
    private void routeToUser(Long toUserId, Message msg, long clientSeq) {
        // 1. 消息落库
        messageService.sendMessage(msg);

        // 2. 推送（对方在线则直接推，离线则存离线消息）
        Set<Channel> channels = sessionManager.getUserChannels(toUserId);
        ImPacket push = ImPacket.push(msg);
        String pushJson = JSON.toJSONString(push);

        if (!channels.isEmpty()) {
            for (Channel ch : channels) {
                if (ch.isActive()) {
                    ch.writeAndFlush(new TextWebSocketFrame(pushJson));
                }
            }
        }
        // 离线消息由 task 模块异步处理

        // 3. 给发送方 ACK（含服务端生成的 msgId）
        ImPacket ack = new ImPacket(ImPacket.CMD_PRIVATE_MSG_ACK, clientSeq,
                System.currentTimeMillis(), msg);
        // ack 在调用方 channel 上发送
    }

    /**
     * 群聊消息广播
     */
    private void routeToGroup(Long groupId, Message msg, long clientSeq) {
        // 1. 消息落库
        messageService.sendMessage(msg);

        // 2. 查询群内所有在线成员并推送
        // TODO: 从 group_member 表查成员列表，逐个 push
        // 此处简化：只推在线成员
        ImPacket push = ImPacket.push(msg);
        String pushJson = JSON.toJSONString(push);

        // 群成员的在线 Channel 由 SessionManager 管理
        // 实际实现需从 GroupMember 表查出所有成员ID，逐个推送
    }

    /**
     * 已读通知处理
     */
    public void handleReadNotify(ChannelSession session, ImPacket packet) {
        JSONObject body = (JSONObject) packet.getBody();
        Long conversationId = body.getLong("conversationId");
        Long lastReadSeq = body.getLong("lastReadSeq");
        // TODO: 更新 message_read 表
        log.info("已读回执: userId={}, conversationId={}, lastReadSeq={}",
                session.getUserId(), conversationId, lastReadSeq);
    }
}
