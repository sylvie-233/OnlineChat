package com.sylvie233.service.user;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.repository.entity.UserSession;
import com.sylvie233.repository.mapper.UserSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户多端登录会话服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService extends ServiceImpl<UserSessionMapper, UserSession> {

    private final UserSessionMapper userSessionMapper;

    /**
     * 记录登录会话
     */
    @Transactional
    public UserSession createSession(Long userId, String token, String deviceType,
                                      String deviceName, String deviceId, String clientIp) {
        // 删除同一 token 的旧记录（避免 uk_token 冲突）
        lambdaUpdate()
                .eq(UserSession::getToken, token)
                .remove();

        // 踢掉同设备的旧会话
        lambdaUpdate()
                .eq(UserSession::getUserId, userId)
                .eq(UserSession::getDeviceId, deviceId)
                .set(UserSession::getStatus, 0)
                .update();

        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setToken(token);
        session.setDeviceType(deviceType);
        session.setDeviceName(deviceName);
        session.setDeviceId(deviceId);
        session.setClientIp(clientIp);
        session.setLastActiveTime(LocalDateTime.now());
        session.setExpireTime(LocalDateTime.now().plusDays(7));
        session.setStatus(1);
        save(session);
        return session;
    }

    /**
     * 获取用户所有活跃会话
     */
    public List<UserSession> getActiveSessions(Long userId) {
        return lambdaQuery()
                .eq(UserSession::getUserId, userId)
                .eq(UserSession::getStatus, 1)
                .orderByDesc(UserSession::getLastActiveTime)
                .list();
    }

    /**
     * 强制登出指定会话
     */
    @Transactional
    public void kickSession(Long sessionId, Long userId) {
        UserSession session = getById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return;
        }
        session.setStatus(0);
        updateById(session);
    }

    /**
     * 登出用户所有设备
     */
    @Transactional
    public void kickAllSessions(Long userId) {
        lambdaUpdate()
                .eq(UserSession::getUserId, userId)
                .eq(UserSession::getStatus, 1)
                .set(UserSession::getStatus, 0)
                .update();
    }

    /**
     * 续期会话活跃时间
     */
    @Transactional
    public void refreshSession(Long userId, String token) {
        lambdaUpdate()
                .eq(UserSession::getUserId, userId)
                .eq(UserSession::getToken, token)
                .set(UserSession::getLastActiveTime, LocalDateTime.now())
                .update();
    }
}
