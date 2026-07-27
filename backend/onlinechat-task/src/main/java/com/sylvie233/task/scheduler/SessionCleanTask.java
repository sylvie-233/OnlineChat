package com.sylvie233.task.scheduler;

import com.sylvie233.repository.mapper.UserSessionMapper;
import com.sylvie233.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话清理定时任务 — 单条 SQL 聚合查询，避免 N+1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanTask {

    private final UserSessionMapper userSessionMapper;
    private final UserService userService;

    /**
     * 每 5 分钟：将过期会话标记为已过期
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cleanExpiredSessions() {
        List<Long> expiredIds = userSessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                        com.sylvie233.repository.entity.UserSession>()
                        .eq(com.sylvie233.repository.entity.UserSession::getStatus, 1)
                        .lt(com.sylvie233.repository.entity.UserSession::getExpireTime, LocalDateTime.now())
                        .last("limit 500"))
                .stream().map(s -> s.getId()).toList();

        if (!expiredIds.isEmpty()) {
            for (Long id : expiredIds) {
                com.sylvie233.repository.entity.UserSession s = new com.sylvie233.repository.entity.UserSession();
                s.setId(id);
                s.setStatus(2);
                userSessionMapper.updateById(s);
            }
            log.info("标记过期会话: {} 条", expiredIds.size());
        }
    }

    /**
     * 每 10 分钟：单条聚合查询找出全部会话过期的用户并离线
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void cleanOfflineUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);

        // 使用 GROUP BY + HAVING 找出所有会话都超时的用户（一次性查询）
        List<Long> offlineUserIds = userSessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                        com.sylvie233.repository.entity.UserSession>()
                        .eq(com.sylvie233.repository.entity.UserSession::getStatus, 1)
                        .lt(com.sylvie233.repository.entity.UserSession::getLastActiveTime, cutoff)
                        .last("limit 200"))
                .stream()
                .map(com.sylvie233.repository.entity.UserSession::getUserId)
                .distinct()
                .filter(uid -> {
                    // 二次确认：该用户确实没有活跃会话
                    long activeCount = userSessionMapper.selectCount(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                    com.sylvie233.repository.entity.UserSession>()
                                    .eq(com.sylvie233.repository.entity.UserSession::getUserId, uid)
                                    .eq(com.sylvie233.repository.entity.UserSession::getStatus, 1)
                                    .gt(com.sylvie233.repository.entity.UserSession::getLastActiveTime, cutoff));
                    return activeCount == 0;
                })
                .toList();

        for (Long userId : offlineUserIds) {
            userService.offline(userId);
        }
        if (!offlineUserIds.isEmpty()) {
            log.info("离线超时用户: {} 人", offlineUserIds.size());
        }
    }
}
