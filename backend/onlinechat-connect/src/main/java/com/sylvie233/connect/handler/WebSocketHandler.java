package com.sylvie233.connect.handler;

import com.alibaba.fastjson2.JSON;
import com.sylvie233.connect.protocol.ImPacket;
import com.sylvie233.connect.session.ChannelSession;
import com.sylvie233.connect.session.SessionManager;
import com.sylvie233.connect.router.MessageRouter;
import com.sylvie233.service.cache.RedisCacheService;
import com.sylvie233.service.user.UserService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WebSocket 消息处理器 — 支持心跳/认证/收发消息/已读/撤回/在线状态/正在输入/转发
 */
@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class WebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final SessionManager sessionManager;
    private final MessageRouter messageRouter;
    private final RedisCacheService redisCacheService;
    private final UserService userService;

    private static final AttributeKey<ChannelSession> SESSION_KEY =
            AttributeKey.valueOf("session");

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        // 连接成功，注册channel session
        ChannelSession session = sessionManager.register(channel);

        // channel反向关联session
        channel.attr(SESSION_KEY).set(session);
        log.info("WebSocket 连接建立: {}", session.channelId());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        ChannelSession session = getSession(channel);
        if (session != null) {
            Long userId = session.getUserId();
            String channelId = session.channelId();
            // 内存session解绑
            sessionManager.unbind(channelId);
            // 清理 Redis channel 绑定
            if (userId != null) {
                // redis channel解绑
                redisCacheService.unbindChannel(userId, channelId);
                // 仅当用户完全没有活跃 channel 时才标记离线
                if (sessionManager.getUserChannels(userId).isEmpty()) {
                    redisCacheService.setOffline(userId);
                    userService.offline(userId);
                }
            }
        }
        log.info("WebSocket 连接断开: {}", session != null ? session.channelId() : "unknown");
        ctx.fireChannelInactive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        Channel channel = ctx.channel();
        ChannelSession session = getSession(channel);

        if (frame instanceof PingWebSocketFrame) {
            log.trace("收到 Ping: {}", session.channelId());
            channel.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        if (frame instanceof CloseWebSocketFrame) {
            int statusCode = ((CloseWebSocketFrame) frame).statusCode();
            String reason = ((CloseWebSocketFrame) frame).reasonText();
            log.info("收到关闭帧: channel={}, userId={}, statusCode={}, reason={}",
                    session.channelId(), session.getUserId(), statusCode, reason);
            channel.close();
            return;
        }

        if (frame instanceof BinaryWebSocketFrame) {
            log.warn("收到二进制帧(暂不支持): {}, size={}B", session.channelId(),
                    frame.content().readableBytes());
            return;
        }

        // 文本消息帧
        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            log.debug("收到文本消息: {}, userId={}, size={}B, payload={}",
                    session.channelId(), session.getUserId(), text.length(),
                    text.length() > 500 ? text.substring(0, 500) + "..." : text);
            try {
                ImPacket packet = JSON.parseObject(text, ImPacket.class);
                log.debug("消息解析成功: {}, cmd={}, seq={}", session.channelId(),
                        ImPacket.cmdName(packet.getCmd()), packet.getSeq());
                handlePacket(channel, session, packet);
            } catch (Exception e) {
                log.error("消息解析失败: channel={}, userId={}, payload={}",
                        session.channelId(), session.getUserId(), text, e);
                channel.writeAndFlush(new TextWebSocketFrame(
                        JSON.toJSONString(ImPacket.error(0, "消息格式错误: " + e.getMessage()))));
            }
        }
    }

    // 文本消息帧处理
    private void handlePacket(Channel channel, ChannelSession session, ImPacket packet) {
        session.refreshActive();

        int cmd = packet.getCmd();
        long seq = packet.getSeq();
        Long userId = session.getUserId();
        String channelId = session.channelId();

        log.debug("处理消息包: channel={}, userId={}, cmd={}({}), seq={}",
                channelId, userId, ImPacket.cmdName(cmd), cmd, seq);

        // 消息命令处理逻辑（核心）
        switch (cmd) {
            case ImPacket.CMD_HEARTBEAT -> {
                log.trace("心跳回复: channel={}, userId={}", channelId, userId);
                channel.writeAndFlush(new TextWebSocketFrame(
                        JSON.toJSONString(ImPacket.heartbeatAck())));
                // 刷新 Redis 在线 TTL，防止长连接过期
                if (userId != null) {
                    redisCacheService.refreshOnline(userId);
                }
            }

            case ImPacket.CMD_AUTH -> {
                log.debug("处理认证请求: channel={}, userId={}, seq={}", channelId, userId, seq);

                // 用户、Session绑定
                messageRouter.handleAuth(channel, session, packet);
            }

            case ImPacket.CMD_PRIVATE_MSG,
                 ImPacket.CMD_GROUP_MSG,
                 ImPacket.CMD_FORWARD_MSG -> {
                if (!session.isAuthenticated()) {
                    log.warn("未认证用户尝试发送消息: channel={}, cmd={}({}), seq={}",
                            channelId, ImPacket.cmdName(cmd), cmd, seq);
                    channel.writeAndFlush(new TextWebSocketFrame(
                            JSON.toJSONString(ImPacket.error(seq, "请先登录"))));
                    return;
                }
                log.debug("路由消息: channel={}, userId={}, cmd={}({}), seq={}",
                        channelId, userId, ImPacket.cmdName(cmd), cmd, seq);

                // 消息路由（消息入库、消息推送、消息应答）
                messageRouter.route(channel, session, packet);
            }

            case ImPacket.CMD_READ_NOTIFY -> {
                if (!session.isAuthenticated()) {
                    log.warn("未认证用户尝试已读通知: channel={}", channelId);
                    return;
                }
                log.debug("已读通知: channel={}, userId={}, seq={}", channelId, userId, seq);

                // 标记消息已读
                messageRouter.handleReadNotify(session, packet);
            }

            case ImPacket.CMD_RECALL_NOTIFY -> {
                if (!session.isAuthenticated()) {
                    log.warn("未认证用户尝试撤回消息: channel={}", channelId);
                    return;
                }
                log.debug("撤回通知: channel={}, userId={}, seq={}", channelId, userId, seq);
                // 消息撤回并广播
                messageRouter.handleRecallNotify(session, packet);
            }

            case ImPacket.CMD_ONLINE_NOTIFY -> {
                if (!session.isAuthenticated()) {
                    log.warn("未认证用户尝试在线状态通知: channel={}", channelId);
                    return;
                }
                log.debug("在线状态通知: channel={}, userId={}, seq={}", channelId, userId, seq);

                // 更新在线状态
                messageRouter.handleOnlineStatusNotify(session, packet);
            }

            case ImPacket.CMD_TYPING -> {
                if (!session.isAuthenticated()) {
                    log.warn("未认证用户尝试正在输入通知: channel={}", channelId);
                    return;
                }
                log.trace("正在输入: channel={}, userId={}, seq={}", channelId, userId, seq);

                // 输入提示推送
                messageRouter.handleTyping(session, packet);
            }

            default -> log.warn("未知命令: channel={}, userId={}, cmd={}({}), seq={}",
                    channelId, userId, ImPacket.cmdName(cmd), cmd, seq);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Channel 异常: {}", ctx.channel().id().asShortText(), cause);
        ctx.close();
    }

    private ChannelSession getSession(Channel channel) {
        return channel.attr(SESSION_KEY).get();
    }
}
