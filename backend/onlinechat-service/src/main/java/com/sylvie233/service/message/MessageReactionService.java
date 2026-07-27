package com.sylvie233.service.message;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.repository.entity.MessageReaction;
import com.sylvie233.repository.mapper.MessageReactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息表情 Reaction 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageReactionService extends ServiceImpl<MessageReactionMapper, MessageReaction> {

    private final MessageReactionMapper reactionMapper;

    /**
     * 添加 Reaction
     */
    @Transactional
    public MessageReaction addReaction(Long messageId, Long userId, String emoji) {
        // 检查重复
        MessageReaction exist = lambdaQuery()
                .eq(MessageReaction::getMessageId, messageId)
                .eq(MessageReaction::getUserId, userId)
                .eq(MessageReaction::getEmoji, emoji)
                .one();
        if (exist != null) {
            return exist;
        }

        MessageReaction reaction = new MessageReaction();
        reaction.setMessageId(messageId);
        reaction.setUserId(userId);
        reaction.setEmoji(emoji);
        save(reaction);
        return reaction;
    }

    /**
     * 移除 Reaction
     */
    @Transactional
    public void removeReaction(Long messageId, Long userId, String emoji) {
        lambdaUpdate()
                .eq(MessageReaction::getMessageId, messageId)
                .eq(MessageReaction::getUserId, userId)
                .eq(MessageReaction::getEmoji, emoji)
                .remove();
    }

    /**
     * 获取消息的所有 Reaction（按 emoji 聚合）
     */
    public Map<String, List<MessageReaction>> getReactions(Long messageId) {
        List<MessageReaction> reactions = lambdaQuery()
                .eq(MessageReaction::getMessageId, messageId)
                .list();
        return reactions.stream()
                .collect(Collectors.groupingBy(MessageReaction::getEmoji));
    }

    /**
     * 获取消息 Reaction 总数
     */
    public long getReactionCount(Long messageId) {
        return lambdaQuery()
                .eq(MessageReaction::getMessageId, messageId)
                .count();
    }
}
