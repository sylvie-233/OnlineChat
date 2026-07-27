package com.sylvie233.task.scheduler;

import com.sylvie233.repository.entity.Message;
import com.sylvie233.repository.mapper.MessageMapper;
import com.sylvie233.service.message.MessageArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息归档定时任务 — 归档完成后删除热表数据，实现真正的冷热分离
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageArchiveTask {

    private final MessageMapper messageMapper;
    private final MessageArchiveService archiveService;

    private static final int BATCH_SIZE = 1000;
    private static final int ARCHIVE_MONTHS = 3;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void archiveOldMessages() {
        log.info("开始消息归档...");
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(ARCHIVE_MONTHS);

        List<Message> oldMessages = messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Message>()
                        .lt(Message::getCreateTime, cutoff)
                        .orderByAsc(Message::getCreateTime)
                        .last("limit " + BATCH_SIZE));

        if (oldMessages.isEmpty()) {
            log.info("无待归档消息");
            return;
        }

        try {
            // 1. 写入归档表
            archiveService.batchArchive(oldMessages);
            // 2. 从热表删除（冷热分离）
            List<Long> ids = oldMessages.stream().map(Message::getId).toList();
            messageMapper.deleteByIds(ids);
            log.info("消息归档完成: 归档+删除 {} 条", oldMessages.size());
        } catch (Exception e) {
            log.error("消息归档失败", e);
        }
    }
}
