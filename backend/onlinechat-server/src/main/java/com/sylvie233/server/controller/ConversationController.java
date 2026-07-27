package com.sylvie233.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.Conversation;
import com.sylvie233.service.conversation.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 会话接口 — 聊天列表、置顶、免打扰、隐藏、草稿、删除
 */
@Tag(name = "会话")
@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @Operation(summary = "获取聊天列表")
    @GetMapping("/list")
    public Result<List<Conversation>> getConversations() {
        return Result.ok(conversationService.getConversations(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "置顶/取消置顶")
    @PutMapping("/{id}/pin")
    public Result<?> togglePin(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!conversationService.isOwner(id, userId)) return Result.fail(403, "无权操作");
        conversationService.setPinned(id, body.getOrDefault("pinned", false));
        return Result.ok();
    }

    @Operation(summary = "免打扰/取消免打扰")
    @PutMapping("/{id}/mute")
    public Result<?> toggleMute(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!conversationService.isOwner(id, userId)) return Result.fail(403, "无权操作");
        conversationService.setMuted(id, body.getOrDefault("muted", false));
        return Result.ok();
    }

    @Operation(summary = "清除未读数")
    @PutMapping("/{id}/clear-unread")
    public Result<?> clearUnread(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!conversationService.isOwner(id, userId)) return Result.fail(403, "无权操作");
        conversationService.clearUnread(id);
        return Result.ok();
    }

    @Operation(summary = "保存草稿")
    @PutMapping("/{id}/draft")
    public Result<?> saveDraft(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!conversationService.isOwner(id, userId)) return Result.fail(403, "无权操作");
        conversationService.saveDraft(id, body.get("draft"));
        return Result.ok();
    }

    @Operation(summary = "隐藏会话")
    @PutMapping("/{id}/hide")
    public Result<?> hideConversation(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!conversationService.isOwner(id, userId)) return Result.fail(403, "无权操作");
        conversationService.setHidden(id, true);
        return Result.ok();
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/{id}")
    public Result<?> deleteConversation(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!conversationService.isOwner(id, userId)) return Result.fail(403, "无权操作");
        conversationService.deleteConversation(id);
        return Result.ok();
    }
}
