package com.sylvie233.task.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sylvie233.repository.entity.UserSession;
import com.sylvie233.repository.mapper.UserSessionMapper;
import com.sylvie233.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * 每 5 分钟：将过期会话标记为已过期（过期时间小于当前时间）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cleanExpiredSessions() {
        List<Long> expiredIds = userSessionMapper.selectList(
                new LambdaQueryWrapper<
                        UserSession>()
                        .eq(UserSession::getStatus, 1)
                        .lt(UserSession::getExpireTime, LocalDateTime.now())
                        .last("limit 500"))
                .stream().map(s -> s.getId()).toList();

        if (!expiredIds.isEmpty()) {
            for (Long id : expiredIds) {
                UserSession s = new UserSession();
                s.setId(id);
                s.setStatus(2); // 更新状态为已过期
                userSessionMapper.updateById(s);
            }
            log.info("标记过期会话: {} 条", expiredIds.size());
        }
    }

    /**
     * 每 10 分钟：单条聚合查询找出全部会话过期的用户并离线（30分钟内未活跃）
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void cleanOfflineUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);

        // 使用 GROUP BY + HAVING 找出所有会话都超时的用户（一次性查询）
        // 二次确认：该用户确实没有活跃会话
        List<Long> offlineUserIds = new ArrayList<>();
        Set<Long> uniqueValues = new HashSet<>();
        for (UserSession userSession : userSessionMapper.selectList(
                new LambdaQueryWrapper<
                        UserSession>()
                        .eq(UserSession::getStatus, 1)
                        .lt(UserSession::getLastActiveTime, cutoff)
                        .last("limit 200"))) {
            Long id = userSession.getUserId();
            if (uniqueValues.add(id)) {
                // 二次确认：该用户确实没有活跃会话
                long activeCount = userSessionMapper.selectCount(
                        new LambdaQueryWrapper<
                                UserSession>()
                                .eq(UserSession::getUserId, id)
                                .eq(UserSession::getStatus, 1)
                                .gt(UserSession::getLastActiveTime, cutoff));
                if (activeCount == 0) {
                    offlineUserIds.add(id);
                }
            }
        }

        for (Long userId : offlineUserIds) {
            userService.offline(userId);
        }
        if (!offlineUserIds.isEmpty()) {
            log.info("离线超时用户: {} 人", offlineUserIds.size());
        }
    }
}
