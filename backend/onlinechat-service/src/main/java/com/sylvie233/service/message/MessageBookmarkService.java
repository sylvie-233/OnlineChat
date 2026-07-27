package com.sylvie233.service.message;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.common.exception.BizException;
import com.sylvie233.repository.entity.Message;
import com.sylvie233.repository.entity.MessageBookmark;
import com.sylvie233.repository.mapper.MessageBookmarkMapper;
import com.sylvie233.repository.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 消息收藏服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageBookmarkService extends ServiceImpl<MessageBookmarkMapper, MessageBookmark> {

    private final MessageBookmarkMapper bookmarkMapper;
    private final MessageMapper messageMapper;

    /**
     * 收藏消息
     */
    @Transactional
    public MessageBookmark bookmark(Long userId, Long messageId, String tag) {
        Message msg = messageMapper.selectById(messageId);
        if (msg == null) {
            throw BizException.of(404, "消息不存在");
        }

        MessageBookmark exist = lambdaQuery()
                .eq(MessageBookmark::getUserId, userId)
                .eq(MessageBookmark::getMessageId, messageId)
                .one();
        if (exist != null) {
            throw BizException.of("已收藏该消息");
        }

        MessageBookmark bookmark = new MessageBookmark();
        bookmark.setUserId(userId);
        bookmark.setMessageId(messageId);
        bookmark.setTag(tag);
        save(bookmark);
        return bookmark;
    }

    /**
     * 取消收藏
     */
    @Transactional
    public void removeBookmark(Long bookmarkId, Long userId) {
        MessageBookmark bookmark = getById(bookmarkId);
        if (bookmark == null || !bookmark.getUserId().equals(userId)) {
            throw BizException.of(403, "无权操作");
        }
        removeById(bookmarkId);
    }

    /**
     * 获取收藏列表（分页）
     */
    public List<MessageBookmark> getBookmarks(Long userId, int page, int size) {
        return lambdaQuery()
                .eq(MessageBookmark::getUserId, userId)
                .orderByDesc(MessageBookmark::getCreateTime)
                .page(new Page<>(page, size))
                .getRecords();
    }

    /**
     * 按标签筛选收藏
     */
    public List<MessageBookmark> getBookmarksByTag(Long userId, String tag, int page, int size) {
        return lambdaQuery()
                .eq(MessageBookmark::getUserId, userId)
                .eq(MessageBookmark::getTag, tag)
                .orderByDesc(MessageBookmark::getCreateTime)
                .page(new Page<>(page, size))
                .getRecords();
    }

    /**
     * 获取用户所有收藏标签
     */
    public List<String> getTags(Long userId) {
        return lambdaQuery()
                .eq(MessageBookmark::getUserId, userId)
                .list()
                .stream()
                .map(MessageBookmark::getTag)
                .filter(tag -> tag != null && !tag.isEmpty())
                .distinct()
                .toList();
    }
}
