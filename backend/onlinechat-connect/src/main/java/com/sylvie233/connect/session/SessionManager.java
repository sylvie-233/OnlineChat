package com.sylvie233.connect.session;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 会话管理器 — Channel ↔ User 映射，支持多端同时在线
 */
@Slf4j
@Component
public class SessionManager {

    private final Map<String, ChannelSession> channelSessionMap = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> userChannelsMap = new ConcurrentHashMap<>();

    /** 注册新 Channel */
    public ChannelSession register(Channel channel) {
        ChannelSession session = new ChannelSession(channel);
        channelSessionMap.put(session.channelId(), session);
        log.info("Channel 注册: {}", session.channelId());
        return session;
    }

    /**
     * 绑定用户 — 同设备踢下线（单设备单登录）
     * 修复：先收集要关闭的 channel，再关闭，避免迭代中修改集合
     */
    public void bindUser(String channelId, Long userId, String deviceType, String deviceId) {
        ChannelSession session = channelSessionMap.get(channelId);
        // 绑定要求Channel已经注册过了
        if (session == null) return;

        // 收集同设备旧连接（不在迭代中关闭 channel）
        List<Channel> toClose = new ArrayList<>();
        Set<String> existingChannels = userChannelsMap.get(userId);
        if (existingChannels != null) {
            for (String cid : existingChannels) {
                ChannelSession s = channelSessionMap.get(cid);
                if (s != null && deviceId != null && deviceId.equals(s.getDeviceId())) {
                    toClose.add(s.getChannel());
                }
            }
        }

        // 安全关闭旧连接
        for (Channel ch : toClose) {
            log.info("同设备踢下线: userId={}, deviceId={}, oldChannel={}", userId, deviceId, ch.id().asShortText());
            ch.close();
        }

        session.setUserId(userId);
        session.setDeviceType(deviceType);
        session.setDeviceId(deviceId);
        session.setAuthenticated(true);

        userChannelsMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(channelId);
        log.info("用户绑定: userId={}, device={}, channel={}", userId, deviceType, channelId);
    }

    /** 解绑 Channel */
    public void unbind(String channelId) {
        ChannelSession session = channelSessionMap.remove(channelId);
        if (session != null && session.getUserId() != null) {
            Set<String> channels = userChannelsMap.get(session.getUserId());
            if (channels != null) {
                channels.remove(channelId);
                if (channels.isEmpty()) userChannelsMap.remove(session.getUserId());
            }
            log.info("用户解绑: userId={}, channel={}", session.getUserId(), channelId);
        }
    }

    /** 获取用户的所有活跃 Channel */
    public Set<Channel> getUserChannels(Long userId) {
        Set<String> channelIds = userChannelsMap.get(userId);
        if (channelIds == null) return Set.of();
        return channelIds.stream()
                .map(channelSessionMap::get)
                .filter(s -> s != null && s.isActive())
                .map(ChannelSession::getChannel)
                .collect(Collectors.toSet());
    }

    public ChannelSession getSession(String channelId) {
        return channelSessionMap.get(channelId);
    }

    public int onlineCount() {
        return userChannelsMap.size();
    }
}
