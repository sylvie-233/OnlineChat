package com.sylvie233.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务
 * <p>管理在线状态、Channel 映射、消息缓存等</p>
 */
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== 在线状态 ====================
    private static final String ONLINE_KEY_PREFIX = "im:online:";
    private static final String USER_CHANNEL_KEY = "im:channel:";  // userId -> serverNode:channelId

    /**
     * 设置用户在线状态
     */
    public void setOnline(Long userId, String serverNode) {
        redisTemplate.opsForValue().set(ONLINE_KEY_PREFIX + userId, serverNode, 30, TimeUnit.MINUTES);
    }

    /**
     * 设置用户离线
     */
    public void setOffline(Long userId) {
        redisTemplate.delete(ONLINE_KEY_PREFIX + userId);
        redisTemplate.delete(USER_CHANNEL_KEY + userId);
    }

    /**
     * 查询用户在线状态
     */
    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_KEY_PREFIX + userId));
    }

    /**
     * 获取用户所在的 Netty 服务器节点
     */
    public String getServerNode(Long userId) {
        Object val = redisTemplate.opsForValue().get(ONLINE_KEY_PREFIX + userId);
        return val != null ? val.toString() : null;
    }

    /**
     * 续期（心跳）
     */
    public void refreshOnline(Long userId) {
        redisTemplate.expire(ONLINE_KEY_PREFIX + userId, 30, TimeUnit.MINUTES);
        redisTemplate.expire(USER_CHANNEL_KEY + userId, 30, TimeUnit.MINUTES);
    }

    // ==================== Channel 映射 ====================

    /**
     * 绑定 userId -> ChannelId
     */
    public void bindChannel(Long userId, String channelId) {
        redisTemplate.opsForValue().set(USER_CHANNEL_KEY + userId, channelId, 30, TimeUnit.MINUTES);
    }

    /**
     * 获取用户的 ChannelId
     */
    public String getChannelId(Long userId) {
        Object val = redisTemplate.opsForValue().get(USER_CHANNEL_KEY + userId);
        return val != null ? val.toString() : null;
    }

    // ==================== 消息 seq ====================

    private static final String MSG_SEQ_KEY = "im:seq:";

    /**
     * 获取会话的下一个消息序号（自增）
     */
    public long nextSeq(Long conversationId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(MSG_SEQ_KEY + conversationId))
                ? redisTemplate.opsForValue().increment(MSG_SEQ_KEY + conversationId)
                : 1L;
    }
}
