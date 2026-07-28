package com.sylvie233.service.notification;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.repository.entity.Notification;
import com.sylvie233.repository.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService extends ServiceImpl<NotificationMapper, Notification> {

    private final NotificationMapper notificationMapper;

    /**
     * 发送通知
     */
    @Transactional
    public Notification send(Long userId, Integer type, String title,
                              String content, Long relatedId) {
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setType(type);
        notif.setTitle(title);
        notif.setContent(content);
        notif.setRelatedId(relatedId);
        notif.setIsRead(0);
        save(notif);
        return notif;
    }

    /**
     * 分页获取用户通知
     */
    public Page<Notification> getNotifications(Long userId, int page, int size, String keyword, Integer isRead) {
        var query = lambdaQuery().eq(Notification::getUserId, userId);
        if (keyword != null && !keyword.isBlank()) {
            query.and(w -> w.like(Notification::getTitle, keyword)
                    .or().like(Notification::getContent, keyword));
        }
        if (isRead != null) {
            query.eq(Notification::getIsRead, isRead);
        }
        return query.orderByDesc(Notification::getCreateTime)
                .page(new Page<>(page, size));
    }

    /**
     * 获取未读通知数
     */
    public long getUnreadCount(Long userId) {
        return lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .count();
    }

    /**
     * 标记已读
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notif = new Notification();
        notif.setId(notificationId);
        notif.setIsRead(1);
        notif.setReadTime(LocalDateTime.now());
        updateById(notif);
    }

    /**
     * 全部标记已读
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        lambdaUpdate()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1)
                .set(Notification::getReadTime, LocalDateTime.now())
                .update();
    }
}
