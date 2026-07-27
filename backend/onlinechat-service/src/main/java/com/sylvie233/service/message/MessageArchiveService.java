package com.sylvie233.service.message;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.repository.entity.Message;
import com.sylvie233.repository.entity.MessageArchive;
import com.sylvie233.repository.mapper.MessageArchiveMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息归档服务 — 冷热分离
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageArchiveService extends ServiceImpl<MessageArchiveMapper, MessageArchive> {

    private final MessageArchiveMapper archiveMapper;

    /**
     * 归档单条消息
     */
    @Transactional
    public void archiveMessage(Message msg) {
        MessageArchive archive = new MessageArchive();
        archive.setId(msg.getId());
        archive.setSeq(msg.getSeq());
        archive.setConversationType(msg.getConversationType());
        archive.setConversationId(msg.getConversationId());
        archive.setFromUserId(msg.getFromUserId());
        archive.setToId(msg.getToId());
        archive.setMsgType(msg.getMsgType());
        archive.setContent(msg.getContent());
        archive.setExtra(msg.getExtra());
        archive.setSendTime(msg.getSendTime());
        archive.setCreateTime(msg.getCreateTime());
        archive.setArchiveTime(LocalDateTime.now());
        save(archive);
    }

    /**
     * 批量归档消息（超过 N 个月的消息迁移到归档表）
     */
    @Transactional
    public void batchArchive(List<Message> messages) {
        for (Message msg : messages) {
            archiveMessage(msg);
        }
        log.info("批量归档 {} 条消息", messages.size());
    }
}
