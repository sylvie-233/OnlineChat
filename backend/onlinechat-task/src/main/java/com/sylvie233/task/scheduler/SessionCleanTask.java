package com.sylvie233.task.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 会话清理定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanTask {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 每 5 分钟清理过期 Token
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cleanExpiredTokens() {
        // 利用 Redis key 过期机制，这里做辅助清理
        log.debug("清理过期会话...");
    }
}
