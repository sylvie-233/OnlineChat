package com.sylvie233.connect.session;

import io.netty.channel.Channel;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Channel 会话封装 — 一个设备连接对应一个 ChannelSession
 * <p>authenticated 字段使用 volatile 保证跨 Netty EventLoop 线程可见性</p>
 */
@Data
public class ChannelSession {

    private Channel channel;
    private Long userId;
    private String deviceType;
    private String deviceId;
    private LocalDateTime connectTime;
    private LocalDateTime lastActiveTime;
    /** volatile: Netty EventLoop 线程写入后对 WebSocketHandler 线程立即可见 */
    private volatile boolean authenticated;

    public ChannelSession(Channel channel) {
        this.channel = channel;
        this.connectTime = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
        this.authenticated = false;
    }

    public void refreshActive() {
        this.lastActiveTime = LocalDateTime.now();
    }

    /** 返回 Netty channel 短 ID */
    public String channelId() {
        return channel.id().asShortText();
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }
}
