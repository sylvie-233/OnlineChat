package com.sylvie233.service.conversation;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.repository.entity.Conversation;
import com.sylvie233.repository.mapper.ConversationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会话服务 — 管理用户聊天列表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService extends ServiceImpl<ConversationMapper, Conversation> {

    private final ConversationMapper conversationMapper;

    /**
     * 更新会话最后一条消息 & 未读计数 +1
     */
    @Transactional
    public void updateLastMessage(Conversation conv) {
        Conversation exist = lambdaQuery()
                .eq(Conversation::getUserId, conv.getUserId())
                .eq(Conversation::getType, conv.getType())
                .eq(Conversation::getTargetId, conv.getTargetId())
                .one();

        if (exist != null) {
            exist.setLastMessageId(conv.getLastMessageId());
            exist.setLastMessageSeq(conv.getLastMessageSeq());
            exist.setUnreadCount(exist.getUnreadCount() + 1);
            updateById(exist);
        } else {
            // 新会话
            conv.setUnreadCount(1);
            save(conv);
        }
    }

    /**
     * 清除未读计数
     */
    @Transactional
    public void clearUnread(Long conversationId) {
        Conversation conv = new Conversation();
        conv.setId(conversationId);
        conv.setUnreadCount(0);
        updateById(conv);
    }
}
