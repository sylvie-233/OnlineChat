package com.sylvie233.connect.session;

import io.netty.channel.Channel;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Channel 会话封装
 * <p>一个设备连接对应一个 ChannelSession</p>
 */
@Data
public class ChannelSession {

    /** Netty Channel */
    private Channel channel;

    /** 用户 ID（认证后绑定） */
    private Long userId;

    /** 设备类型: web/ios/android/desktop */
    private String deviceType;

    /** 设备唯一标识 */
    private String deviceId;

    /** 连接时间 */
    private LocalDateTime connectTime;

    /** 最后活跃时间 */
    private LocalDateTime lastActiveTime;

    /** 是否已认证 */
    private boolean authenticated;

    public ChannelSession(Channel channel) {
        this.channel = channel;
        this.connectTime = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
        this.authenticated = false;
    }

    public void refreshActive() {
        this.lastActiveTime = LocalDateTime.now();
    }

    public String channelId() {
        return channel.id().asShortText();
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }
}
