package com.sylvie233.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis 缓存服务 — 在线状态、Channel映射、消息seq、离线消息
 */
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== 在线状态 ====================
    private static final String ONLINE_KEY_PREFIX = "im:online:";
    private static final String USER_CHANNEL_KEY = "im:channel:";

    // 设置在线
    public void setOnline(Long userId, String serverNode) {
        redisTemplate.opsForValue().set(ONLINE_KEY_PREFIX + userId, serverNode, 30, TimeUnit.MINUTES);
    }

    // 设置下线
    public void setOffline(Long userId) {
        redisTemplate.delete(ONLINE_KEY_PREFIX + userId);
        redisTemplate.delete(USER_CHANNEL_KEY + userId);
    }

    // 判断是否在线
    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_KEY_PREFIX + userId));
    }

    // 获取用户所在节点
    public String getServerNode(Long userId) {
        Object val = redisTemplate.opsForValue().get(ONLINE_KEY_PREFIX + userId);
        return val != null ? val.toString() : null;
    }

    // 刷新在线
    public void refreshOnline(Long userId) {
        redisTemplate.expire(ONLINE_KEY_PREFIX + userId, 30, TimeUnit.MINUTES);
        redisTemplate.expire(USER_CHANNEL_KEY + userId, 30, TimeUnit.MINUTES);
    }

    // ==================== Channel 映射 ====================

    /** 绑定用户 channel（支持多设备） */
    public void bindChannel(Long userId, String channelId) {
        String key = USER_CHANNEL_KEY + userId;
        redisTemplate.opsForSet().add(key, channelId);
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);
    }

    /** 解绑用户 channel */
    public void unbindChannel(Long userId, String channelId) {
        String key = USER_CHANNEL_KEY + userId;
        redisTemplate.opsForSet().remove(key, channelId);
    }

    /** 获取用户所有 channel */
    public Set<String> getChannelIds(Long userId) {
        Set<Object> members = redisTemplate.opsForSet().members(USER_CHANNEL_KEY + userId);
        if (members == null || members.isEmpty()) return Set.of();
        return members.stream().map(Object::toString).collect(Collectors.toSet());
    }

    // ==================== 消息 seq ====================

    private static final String MSG_SEQ_KEY = "im:seq:";

    /**
     * 获取下一个消息序号（自增），按 seqKey 维度
     */
    public long nextSeq(String seqKey) {
        Long seq = redisTemplate.opsForValue().increment(MSG_SEQ_KEY + seqKey);
        return seq != null ? seq : 1L;
    }

    /**
     * 获取当前 seq（不自增）
     */
    public long currentSeq(String seqKey) {
        Object val = redisTemplate.opsForValue().get(MSG_SEQ_KEY + seqKey);
        return val != null ? Long.parseLong(val.toString()) : 0L;
    }

    // ==================== 离线消息 ====================

    private static final String OFFLINE_MSG_KEY = "im:offline:";

    /**
     * 存储离线消息到 Redis
     */
    public void storeOfflineMessage(Long userId, Object msg) {
        String key = OFFLINE_MSG_KEY + userId;
        redisTemplate.opsForList().rightPush(key, msg);
        redisTemplate.expire(key, 7, TimeUnit.DAYS);
    }

    /**
     * 获取并清除离线消息
     */
    public List<Object> fetchOfflineMessages(Long userId) {
        String key = OFFLINE_MSG_KEY + userId;
        List<Object> messages = new ArrayList<>();
        while (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            Object msg = redisTemplate.opsForList().leftPop(key);
            if (msg == null) break;
            messages.add(msg);
        }
        return messages;
    }

    /**
     * 离线消息数量
     */
    public long offlineMessageCount(Long userId) {
        String key = OFFLINE_MSG_KEY + userId;
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0;
    }

    // ==================== 消息发送限流 ====================

    private static final String RATE_LIMIT_KEY = "im:ratelimit:";

    /**
     * 检查消息发送频率（默认每秒最多5条）
     */
    public boolean checkRateLimit(Long userId, int maxPerSecond) {
        String key = RATE_LIMIT_KEY + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.SECONDS);
        }
        return count != null && count <= maxPerSecond;
    }
}
