package com.sylvie233.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sylvie233.common.enums.ConversationType;
import com.sylvie233.common.model.dto.BookmarkRequest;
import com.sylvie233.common.model.dto.ReactionRequest;
import com.sylvie233.common.model.dto.SendMessageRequest;
import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.Message;
import com.sylvie233.repository.entity.MessageBookmark;
import com.sylvie233.repository.entity.MessageReaction;
import com.sylvie233.service.message.MessageService;
import com.sylvie233.service.message.MessageReadService;
import com.sylvie233.service.message.MessageReactionService;
import com.sylvie233.service.message.MessageBookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息接口 — 发送、历史查询、撤回、已读、Reaction、收藏、@提及
 */
@Tag(name = "消息")
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final MessageReadService messageReadService;
    private final MessageReactionService reactionService;
    private final MessageBookmarkService bookmarkService;

    @Operation(summary = "发送消息(HTTP降级)")
    @PostMapping("/send")
    public Result<Message> sendMessage(@RequestBody SendMessageRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        Message msg = new Message();
        msg.setFromUserId(userId);
        msg.setConversationType(req.getConversationType());
        msg.setConversationId(req.getConversationId());
        msg.setToId(req.getToId());
        msg.setMsgType(req.getMsgType());
        msg.setContent(req.getContent());
        msg.setExtra(req.getExtra());
        msg.setClientMsgId(req.getClientMsgId());
        msg.setReplyToMsgId(req.getReplyToMsgId());
        msg.setStatus(0);
        return Result.ok(messageService.sendMessage(msg));
    }

    @Operation(summary = "拉取最新消息")
    @GetMapping("/latest")
    public Result<List<Message>> getLatest(@RequestParam Long conversationId,
                                            @RequestParam(defaultValue = "0") int type,
                                            @RequestParam(defaultValue = "20") int limit) {
        ConversationType convType = ConversationType.values()[type];
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(messageService.getLatestMessages(conversationId, convType, limit, userId));
    }

    @Operation(summary = "拉取历史消息")
    @GetMapping("/history")
    public Result<List<Message>> getHistory(@RequestParam Long conversationId,
                                             @RequestParam(defaultValue = "0") int type,
                                             @RequestParam String cursorTime,
                                             @RequestParam(defaultValue = "20") int limit) {
        ConversationType convType = ConversationType.values()[type];
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(messageService.getHistoryMessages(conversationId, convType, cursorTime, limit, userId));
    }

    @Operation(summary = "增量同步(sinceTime游标): 拉取 sinceTime 之后的新消息")
    @GetMapping("/sync")
    public Result<List<Message>> sync(@RequestParam Long conversationId,
                                       @RequestParam(defaultValue = "0") int type,
                                       @RequestParam String sinceTime,
                                       @RequestParam(defaultValue = "50") int limit) {
        try {
            ConversationType convType = ConversationType.values()[type];
            Long userId = StpUtil.getLoginIdAsLong();
            return Result.ok(messageService.getSyncMessages(conversationId, convType, sinceTime, limit, userId));
        } catch (IndexOutOfBoundsException e) {
            return Result.fail("无效的会话类型");
        }
    }

    @Operation(summary = "撤回消息")
    @PutMapping("/{messageId}/recall")
    public Result<?> recallMessage(@PathVariable Long messageId,
                                    @RequestBody(required = false) Map<String, String> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        String reason = body != null ? body.get("reason") : null;
        return messageService.recallMessage(messageId, userId, reason)
                ? Result.ok("已撤回") : Result.fail("撤回失败：超过2分钟或无权操作");
    }

    @Operation(summary = "标记消息已读")
    @PutMapping("/{messageId}/read")
    public Result<?> markAsRead(@PathVariable Long messageId) {
        Long userId = StpUtil.getLoginIdAsLong();
        messageReadService.markAsRead(messageId, userId);
        return Result.ok();
    }

    @Operation(summary = "获取消息已读人数")
    @GetMapping("/{messageId}/read-count")
    public Result<Long> getReadCount(@PathVariable Long messageId) {
        return Result.ok(messageReadService.getReadCount(messageId));
    }

    @Operation(summary = "获取消息已读用户列表")
    @GetMapping("/{messageId}/read-users")
    public Result<List<Long>> getReadUsers(@PathVariable Long messageId) {
        return Result.ok(messageReadService.getReadUserIds(messageId));
    }

    @Operation(summary = "添加表情Reaction")
    @PostMapping("/{messageId}/reaction")
    public Result<MessageReaction> addReaction(@PathVariable Long messageId,
                                                @RequestBody ReactionRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(reactionService.addReaction(messageId, userId, req.getEmoji()));
    }

    @Operation(summary = "移除表情Reaction")
    @DeleteMapping("/{messageId}/reaction")
    public Result<?> removeReaction(@PathVariable Long messageId,
                                     @RequestBody ReactionRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        reactionService.removeReaction(messageId, userId, req.getEmoji());
        return Result.ok();
    }

    @Operation(summary = "获取消息Reaction列表")
    @GetMapping("/{messageId}/reactions")
    public Result<Map<String, List<MessageReaction>>> getReactions(@PathVariable Long messageId) {
        return Result.ok(reactionService.getReactions(messageId));
    }

    @Operation(summary = "收藏消息")
    @PostMapping("/{messageId}/bookmark")
    public Result<MessageBookmark> bookmark(@PathVariable Long messageId,
                                             @RequestBody BookmarkRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(bookmarkService.bookmark(userId, messageId, req.getTag()));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/bookmark/{bookmarkId}")
    public Result<?> removeBookmark(@PathVariable Long bookmarkId) {
        Long userId = StpUtil.getLoginIdAsLong();
        bookmarkService.removeBookmark(bookmarkId, userId);
        return Result.ok();
    }

    @Operation(summary = "获取收藏列表")
    @GetMapping("/bookmarks")
    public Result<List<MessageBookmark>> getBookmarks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tag) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (tag != null && !tag.isEmpty()) {
            return Result.ok(bookmarkService.getBookmarksByTag(userId, tag, page, size));
        }
        return Result.ok(bookmarkService.getBookmarks(userId, page, size));
    }

    @Operation(summary = "获取收藏标签列表")
    @GetMapping("/bookmark-tags")
    public Result<List<String>> getBookmarkTags() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(bookmarkService.getTags(userId));
    }

    @Operation(summary = "获取@我的消息")
    @GetMapping("/mentions")
    public Result<?> getMentions(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(messageService.getMentionMessages(userId, page, size));
    }

    @Operation(summary = "获取未读@提及数量")
    @GetMapping("/mentions/unread-count")
    public Result<Long> getUnreadMentionCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(messageService.getUnreadMentionCount(userId));
    }

    // ==================== 消息重发 ====================

    @Operation(summary = "重发失败的消息")
    @PostMapping("/{messageId}/retry")
    public Result<Message> retryMessage(@PathVariable Long messageId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Message retried = messageService.retryMessage(messageId, userId);
        if (retried == null) return Result.fail("重发失败：消息不存在或无权操作");
        return Result.ok(retried);
    }
}
