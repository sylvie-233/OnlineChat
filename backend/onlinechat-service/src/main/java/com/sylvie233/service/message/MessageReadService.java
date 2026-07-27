package com.sylvie233.service.message;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.repository.entity.MessageRead;
import com.sylvie233.repository.mapper.MessageReadMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息已读回执服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageReadService extends ServiceImpl<MessageReadMapper, MessageRead> {

    /**
     * 标记单条消息已读
     */
    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        MessageRead exist = lambdaQuery()
                .eq(MessageRead::getMessageId, messageId)
                .eq(MessageRead::getUserId, userId)
                .one();
        if (exist == null) {
            MessageRead read = new MessageRead();
            read.setMessageId(messageId);
            read.setUserId(userId);
            read.setReadTime(LocalDateTime.now());
            save(read);
        }
    }

    /**
     * 批量标记会话消息已读（写入具体消息级别的已读记录）
     */
    @Transactional
    public void markConversationRead(List<Long> messageIds, Long userId) {
        for (Long messageId : messageIds) {
            markAsRead(messageId, userId);
        }
    }

    /** 获取消息已读人数 */
    public long getReadCount(Long messageId) {
        return lambdaQuery().eq(MessageRead::getMessageId, messageId).count();
    }

    /** 获取消息已读用户 ID 列表 */
    public List<Long> getReadUserIds(Long messageId) {
        return lambdaQuery().eq(MessageRead::getMessageId, messageId)
                .list().stream().map(MessageRead::getUserId).toList();
    }
}
