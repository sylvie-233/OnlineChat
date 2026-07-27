package com.sylvie233.connect.session;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 会话管理器
 * <p>维护 Channel ↔ User 的映射关系，支持单用户多端登录</p>
 */
@Slf4j
@Component
public class SessionManager {

    /** ChannelId -> Session */
    private final Map<String, ChannelSession> channelSessionMap = new ConcurrentHashMap<>();

    /** UserId -> Set<ChannelId> （一个用户可能多个设备在线） */
    private final Map<Long, Set<String>> userChannelsMap = new ConcurrentHashMap<>();

    /**
     * 注册 Channel
     */
    public ChannelSession register(Channel channel) {
        ChannelSession session = new ChannelSession(channel);
        channelSessionMap.put(session.channelId(), session);
        log.info("Channel 注册: {}", session.channelId());
        return session;
    }

    /**
     * 绑定用户（认证成功后调用）
     */
    public void bindUser(String channelId, Long userId, String deviceType, String deviceId) {
        ChannelSession session = channelSessionMap.get(channelId);
        if (session == null) return;

        // 同设备踢下线（单设备单登录）
        userChannelsMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .stream()
                .map(channelSessionMap::get)
                .filter(s -> s != null && deviceId.equals(s.getDeviceId()))
                .forEach(s -> {
                    log.info("同设备踢下线: userId={}, deviceId={}, oldChannel={}", userId, deviceId, s.channelId());
                    s.getChannel().close();
                });

        session.setUserId(userId);
        session.setDeviceType(deviceType);
        session.setDeviceId(deviceId);
        session.setAuthenticated(true);

        userChannelsMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(channelId);
        log.info("用户绑定: userId={}, device={}, channel={}", userId, deviceType, channelId);
    }

    /**
     * 解绑 Channel
     */
    public void unbind(String channelId) {
        ChannelSession session = channelSessionMap.remove(channelId);
        if (session != null && session.getUserId() != null) {
            Set<String> channels = userChannelsMap.get(session.getUserId());
            if (channels != null) {
                channels.remove(channelId);
                if (channels.isEmpty()) {
                    userChannelsMap.remove(session.getUserId());
                }
            }
            log.info("用户解绑: userId={}, channel={}", session.getUserId(), channelId);
        }
    }

    /**
     * 获取用户的所有 Channel
     */
    public Set<Channel> getUserChannels(Long userId) {
        Set<String> channelIds = userChannelsMap.get(userId);
        if (channelIds == null) return Set.of();
        return channelIds.stream()
                .map(channelSessionMap::get)
                .filter(s -> s != null && s.isActive())
                .map(ChannelSession::getChannel)
                .collect(Collectors.toSet());
    }

    /**
     * 获取会话
     */
    public ChannelSession getSession(String channelId) {
        return channelSessionMap.get(channelId);
    }

    /**
     * 在线用户数
     */
    public int onlineCount() {
        return userChannelsMap.size();
    }
}
