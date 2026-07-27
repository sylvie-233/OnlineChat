package com.sylvie233.service.message;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.repository.entity.MessageMention;
import com.sylvie233.repository.mapper.MessageMentionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息 @提及 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageMentionService extends ServiceImpl<MessageMentionMapper, MessageMention> {

    private final MessageMentionMapper mentionMapper;

    /**
     * 批量创建 @提及记录
     */
    @Transactional
    public void createMentions(Long messageId, Long fromUserId, List<Long> toUserIds) {
        for (Long toUserId : toUserIds) {
            MessageMention mention = new MessageMention();
            mention.setMessageId(messageId);
            mention.setFromUserId(fromUserId);
            mention.setToUserId(toUserId);
            mention.setIsRead(0);
            save(mention);
        }
    }

    /**
     * 获取 @我的消息列表
     */
    public List<MessageMention> getMentions(Long userId, int page, int size) {
        return lambdaQuery()
                .eq(MessageMention::getToUserId, userId)
                .orderByDesc(MessageMention::getCreateTime)
                .page(new Page<>(page, size))
                .getRecords();
    }

    /**
     * 获取未读 @提及数量
     */
    public long getUnreadCount(Long userId) {
        return lambdaQuery()
                .eq(MessageMention::getToUserId, userId)
                .eq(MessageMention::getIsRead, 0)
                .count();
    }

    /**
     * 标记 @提及为已读
     */
    @Transactional
    public void markAsRead(Long mentionId, Long userId) {
        lambdaUpdate()
                .eq(MessageMention::getId, mentionId)
                .eq(MessageMention::getToUserId, userId)
                .set(MessageMention::getIsRead, 1)
                .update();
    }
}
