package com.sylvie233.service.message;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.common.enums.ConversationType;
import com.sylvie233.common.enums.MessageStatus;
import com.sylvie233.repository.entity.Message;
import com.sylvie233.repository.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息核心服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService extends ServiceImpl<MessageMapper, Message> {

    private final MessageMapper messageMapper;

    /**
     * 保存并发送消息
     */
    @Transactional
    public Message sendMessage(Message msg) {
        // 幂等检查
        if (msg.getClientMsgId() != null) {
            Message exist = lambdaQuery()
                    .eq(Message::getClientMsgId, msg.getClientMsgId())
                    .one();
            if (exist != null) {
                log.warn("重复消息 clientMsgId={}, 跳过", msg.getClientMsgId());
                return exist;
            }
        }
        msg.setStatus(MessageStatus.SENT.getCode());
        msg.setSendTime(LocalDateTime.now());
        save(msg);
        return msg;
    }

    /**
     * 拉取会话最新消息
     */
    public List<Message> getLatestMessages(Long conversationId, ConversationType type, int limit) {
        return messageMapper.selectLatestByConversation(conversationId, type.getCode(), limit);
    }

    /**
     * 向前翻页拉取历史消息
     */
    public List<Message> getHistoryMessages(Long conversationId, ConversationType type,
                                             Long cursorSeq, int limit) {
        return messageMapper.selectHistoryByConversation(conversationId, type.getCode(), cursorSeq, limit);
    }

    /**
     * 撤回消息（2 分钟内）
     */
    @Transactional
    public boolean recallMessage(Long messageId, Long userId) {
        Message msg = getById(messageId);
        if (msg == null || !msg.getFromUserId().equals(userId)) {
            return false;
        }
        // 2 分钟撤回窗口（业务层控制）
        if (msg.getCreateTime().plusMinutes(2).isBefore(LocalDateTime.now())) {
            return false;
        }
        msg.setIsRecalled(1);
        msg.setStatus(MessageStatus.RECALLED.getCode());
        msg.setRecalledTime(LocalDateTime.now());
        return updateById(msg);
    }
}
