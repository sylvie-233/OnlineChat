package com.sylvie233.connect.handler;

import com.alibaba.fastjson2.JSON;
import com.sylvie233.connect.protocol.ImPacket;
import com.sylvie233.connect.session.ChannelSession;
import com.sylvie233.connect.session.SessionManager;
import com.sylvie233.connect.router.MessageRouter;
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

    private static final AttributeKey<ChannelSession> SESSION_KEY =
            AttributeKey.valueOf("session");

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        ChannelSession session = sessionManager.register(channel);
        channel.attr(SESSION_KEY).set(session);
        log.info("WebSocket 连接建立: {}", session.channelId());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        ChannelSession session = getSession(channel);
        if (session != null) {
            sessionManager.unbind(session.channelId());
        }
        log.info("WebSocket 连接断开: {}", session != null ? session.channelId() : "unknown");
        ctx.fireChannelInactive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        Channel channel = ctx.channel();
        ChannelSession session = getSession(channel);

        if (frame instanceof PingWebSocketFrame) {
            channel.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        if (frame instanceof CloseWebSocketFrame) {
            channel.close();
            return;
        }

        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            try {
                ImPacket packet = JSON.parseObject(text, ImPacket.class);
                handlePacket(channel, session, packet);
            } catch (Exception e) {
                log.error("消息解析失败: {}", text, e);
                channel.writeAndFlush(new TextWebSocketFrame(
                        JSON.toJSONString(ImPacket.error(0, "消息格式错误: " + e.getMessage()))));
            }
        }
    }

    private void handlePacket(Channel channel, ChannelSession session, ImPacket packet) {
        session.refreshActive();

        switch (packet.getCmd()) {
            case ImPacket.CMD_HEARTBEAT -> {
                channel.writeAndFlush(new TextWebSocketFrame(
                        JSON.toJSONString(ImPacket.heartbeatAck())));
            }

            case ImPacket.CMD_AUTH -> {
                messageRouter.handleAuth(channel, session, packet);
            }

            case ImPacket.CMD_PRIVATE_MSG,
                 ImPacket.CMD_GROUP_MSG,
                 ImPacket.CMD_FORWARD_MSG -> {
                if (!session.isAuthenticated()) {
                    channel.writeAndFlush(new TextWebSocketFrame(
                            JSON.toJSONString(ImPacket.error(packet.getSeq(), "请先登录"))));
                    return;
                }
                messageRouter.route(channel, session, packet);
            }

            case ImPacket.CMD_READ_NOTIFY -> {
                if (session.isAuthenticated()) {
                    messageRouter.handleReadNotify(session, packet);
                }
            }

            case ImPacket.CMD_RECALL_NOTIFY -> {
                if (session.isAuthenticated()) {
                    messageRouter.handleRecallNotify(session, packet);
                }
            }

            case ImPacket.CMD_ONLINE_NOTIFY -> {
                if (session.isAuthenticated()) {
                    messageRouter.handleOnlineStatusNotify(session, packet);
                }
            }

            case ImPacket.CMD_TYPING -> {
                if (session.isAuthenticated()) {
                    messageRouter.handleTyping(session, packet);
                }
            }

            default -> log.warn("未知命令: cmd={}", packet.getCmd());
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
