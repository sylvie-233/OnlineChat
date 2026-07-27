package com.sylvie233.service.group;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.common.exception.BizException;
import com.sylvie233.repository.entity.GroupAnnouncement;
import com.sylvie233.repository.entity.GroupAnnouncementRead;
import com.sylvie233.repository.mapper.GroupAnnouncementMapper;
import com.sylvie233.repository.mapper.GroupAnnouncementReadMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群公告服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupAnnouncementService extends ServiceImpl<GroupAnnouncementMapper, GroupAnnouncement> {

    private final GroupAnnouncementMapper announcementMapper;
    private final GroupAnnouncementReadMapper announcementReadMapper;

    /**
     * 发布群公告
     */
    @Transactional
    public GroupAnnouncement publish(Long groupId, Long publisherId, String title, String content) {
        GroupAnnouncement announcement = new GroupAnnouncement();
        announcement.setGroupId(groupId);
        announcement.setPublisherId(publisherId);
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setIsPinned(0);
        save(announcement);
        return announcement;
    }

    /**
     * 编辑群公告
     */
    @Transactional
    public void updateAnnouncement(Long announcementId, Long userId, String title, String content) {
        GroupAnnouncement announcement = getById(announcementId);
        if (announcement == null || !announcement.getPublisherId().equals(userId)) {
            throw BizException.of(403, "无权编辑该公告");
        }
        announcement.setTitle(title);
        announcement.setContent(content);
        updateById(announcement);
    }

    /**
     * 删除群公告
     */
    @Transactional
    public void deleteAnnouncement(Long announcementId, Long userId) {
        GroupAnnouncement announcement = getById(announcementId);
        if (announcement == null || !announcement.getPublisherId().equals(userId)) {
            throw BizException.of(403, "无权删除该公告");
        }
        removeById(announcementId);
    }

    /**
     * 置顶/取消置顶公告
     */
    @Transactional
    public void togglePin(Long announcementId, boolean pinned) {
        GroupAnnouncement announcement = getById(announcementId);
        if (announcement != null) {
            announcement.setIsPinned(pinned ? 1 : 0);
            updateById(announcement);
        }
    }

    /**
     * 获取群公告列表
     */
    public List<GroupAnnouncement> getAnnouncements(Long groupId, int page, int size) {
        return lambdaQuery()
                .eq(GroupAnnouncement::getGroupId, groupId)
                .orderByDesc(GroupAnnouncement::getIsPinned)
                .orderByDesc(GroupAnnouncement::getCreateTime)
                .page(new Page<>(page, size))
                .getRecords();
    }

    /**
     * 标记公告已读
     */
    @Transactional
    public void markAsRead(Long announcementId, Long userId) {
        GroupAnnouncementRead exist = announcementReadMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GroupAnnouncementRead>()
                        .eq(GroupAnnouncementRead::getAnnouncementId, announcementId)
                        .eq(GroupAnnouncementRead::getUserId, userId));
        if (exist == null) {
            GroupAnnouncementRead read = new GroupAnnouncementRead();
            read.setAnnouncementId(announcementId);
            read.setUserId(userId);
            read.setReadTime(LocalDateTime.now());
            announcementReadMapper.insert(read);
        }
    }

    /**
     * 获取公告已读/未读人数
     */
    public long getReadCount(Long announcementId) {
        return announcementReadMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GroupAnnouncementRead>()
                        .eq(GroupAnnouncementRead::getAnnouncementId, announcementId));
    }
}
