package com.sylvie233.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sylvie233.common.model.dto.GroupSettingsRequest;
import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.*;
import com.sylvie233.service.group.GroupService;
import com.sylvie233.service.group.GroupAnnouncementService;
import com.sylvie233.service.group.GroupRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 群组接口 — 创建/解散/设置/成员管理/公告/入群审批
 */
@Tag(name = "群组")
@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final GroupAnnouncementService announcementService;
    private final GroupRequestService groupRequestService;
    private final com.sylvie233.service.message.MessageService messageService;
    private final com.sylvie233.service.message.MessageReadService messageReadService;

    @Operation(summary = "创建群")
    @PostMapping
    public Result<GroupInfo> createGroup(@RequestBody Map<String, String> body) {
        return Result.ok(groupService.createGroup(
                StpUtil.getLoginIdAsLong(), body.get("groupName")));
    }

    @Operation(summary = "获取群信息")
    @GetMapping("/{groupId}")
    public Result<GroupInfo> getGroup(@PathVariable Long groupId) {
        return Result.ok(groupService.getById(groupId));
    }

    @Operation(summary = "更新群设置")
    @PutMapping("/{groupId}/settings")
    public Result<?> updateGroupSettings(@PathVariable Long groupId,
                                          @RequestBody GroupSettingsRequest body) {
        GroupInfo info = new GroupInfo();
        info.setGroupName(body.getGroupName());
        info.setAvatar(body.getAvatar());
        info.setDescription(body.getDescription());
        info.setJoinType(body.getJoinType());
        info.setMaxMembers(body.getMaxMembers());
        info.setIsMutedAll(body.getIsMutedAll());
        groupService.updateSettings(groupId, StpUtil.getLoginIdAsLong(), info);
        return Result.ok("更新成功");
    }

    @Operation(summary = "解散群")
    @DeleteMapping("/{groupId}")
    public Result<?> dismissGroup(@PathVariable Long groupId) {
        groupService.dismissGroup(groupId, StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    @Operation(summary = "获取群成员列表")
    @GetMapping("/{groupId}/members")
    public Result<List<GroupMember>> getMembers(@PathVariable Long groupId) {
        return Result.ok(groupService.getMembers(groupId));
    }

    @Operation(summary = "加入群")
    @PostMapping("/{groupId}/join")
    public Result<?> joinGroup(@PathVariable Long groupId) {
        groupService.joinGroup(groupId, StpUtil.getLoginIdAsLong());
        return Result.ok("加入成功");
    }

    @Operation(summary = "邀请用户入群")
    @PostMapping("/{groupId}/invite/{inviteeId}")
    public Result<?> inviteMember(@PathVariable Long groupId, @PathVariable Long inviteeId) {
        groupService.inviteMember(groupId, StpUtil.getLoginIdAsLong(), inviteeId);
        return Result.ok("已邀请");
    }

    @Operation(summary = "踢出成员")
    @DeleteMapping("/{groupId}/member/{userId}")
    public Result<?> kickMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.kickMember(groupId, StpUtil.getLoginIdAsLong(), userId);
        return Result.ok();
    }

    @Operation(summary = "设置成员角色")
    @PutMapping("/{groupId}/member/{userId}/role")
    public Result<?> setMemberRole(@PathVariable Long groupId, @PathVariable Long userId,
                                    @RequestBody Map<String, Integer> body) {
        groupService.setMemberRole(groupId, StpUtil.getLoginIdAsLong(), userId, body.get("role"));
        return Result.ok();
    }

    @Operation(summary = "设置群内昵称")
    @PutMapping("/{groupId}/member/{userId}/nickname")
    public Result<?> setMemberNickname(@PathVariable Long groupId, @PathVariable Long userId,
                                        @RequestBody Map<String, String> body) {
        groupService.setMemberNickname(groupId, StpUtil.getLoginIdAsLong(), userId, body.get("nickname"));
        return Result.ok();
    }

    @Operation(summary = "更新成员设置")
    @PutMapping("/{groupId}/member/settings")
    public Result<?> updateMemberSettings(@PathVariable Long groupId,
                                           @RequestBody Map<String, Object> body) {
        groupService.updateMemberSettings(groupId, StpUtil.getLoginIdAsLong(), body);
        return Result.ok();
    }

    // ==================== 群公告 ====================

    @Operation(summary = "发布群公告")
    @PostMapping("/{groupId}/announcement")
    public Result<GroupAnnouncement> publishAnnouncement(@PathVariable Long groupId,
                                                           @RequestBody Map<String, String> body) {
        return Result.ok(announcementService.publish(
                groupId, StpUtil.getLoginIdAsLong(), body.get("title"), body.get("content")));
    }

    @Operation(summary = "编辑群公告")
    @PutMapping("/announcement/{announcementId}")
    public Result<?> updateAnnouncement(@PathVariable Long announcementId,
                                         @RequestBody Map<String, String> body) {
        announcementService.updateAnnouncement(
                announcementId, StpUtil.getLoginIdAsLong(), body.get("title"), body.get("content"));
        return Result.ok();
    }

    @Operation(summary = "删除群公告")
    @DeleteMapping("/announcement/{announcementId}")
    public Result<?> deleteAnnouncement(@PathVariable Long announcementId) {
        announcementService.deleteAnnouncement(announcementId, StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    @Operation(summary = "获取群公告列表")
    @GetMapping("/{groupId}/announcements")
    public Result<List<GroupAnnouncement>> getAnnouncements(@PathVariable Long groupId,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return Result.ok(announcementService.getAnnouncements(groupId, page, size));
    }

    @Operation(summary = "标记公告已读")
    @PutMapping("/announcement/{announcementId}/read")
    public Result<?> markAnnouncementRead(@PathVariable Long announcementId) {
        announcementService.markAsRead(announcementId, StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    // ==================== 入群申请/邀请 ====================

    @Operation(summary = "申请加入群")
    @PostMapping("/{groupId}/apply")
    public Result<GroupRequest> applyJoin(@PathVariable Long groupId,
                                           @RequestBody Map<String, String> body) {
        return Result.ok(groupRequestService.applyJoin(
                groupId, StpUtil.getLoginIdAsLong(), body.getOrDefault("verifyMessage", "")));
    }

    @Operation(summary = "处理入群申请")
    @PutMapping("/request/{requestId}")
    public Result<?> handleRequest(@PathVariable Long requestId,
                                    @RequestBody Map<String, Boolean> body) {
        groupRequestService.handleRequest(requestId, StpUtil.getLoginIdAsLong(),
                body.getOrDefault("agree", false));
        return Result.ok();
    }

    @Operation(summary = "获取群申请列表")
    @GetMapping("/{groupId}/requests")
    public Result<List<GroupRequest>> getRequests(@PathVariable Long groupId) {
        return Result.ok(groupRequestService.getRequests(groupId));
    }

    @Operation(summary = "获取收到的入群邀请")
    @GetMapping("/invitations")
    public Result<List<GroupRequest>> getInvitations() {
        return Result.ok(groupRequestService.getInvitations(StpUtil.getLoginIdAsLong()));
    }

    // ==================== 群聊消息已读统计 ====================

    @Operation(summary = "获取群消息已读/未读统计")
    @GetMapping("/message/{messageId}/read-stats")
    public Result<?> getMessageReadStats(@PathVariable Long messageId) {
        long readCount = messageReadService.getReadCount(messageId);
        Message msg = messageService.getById(messageId);
        long totalMembers = groupService.lambdaQuery()
                .eq(com.sylvie233.repository.entity.GroupInfo::getId, msg != null ? msg.getToId() : 0)
                .oneOpt()
                .map(com.sylvie233.repository.entity.GroupInfo::getMemberCount)
                .orElse(0);
        return Result.ok(java.util.Map.of(
                "readCount", readCount,
                "unreadCount", totalMembers - readCount,
                "totalMembers", totalMembers));
    }

    // ==================== 群聊 @我 筛选 ====================

    @Operation(summary = "获取群内@我的消息")
    @GetMapping("/{groupId}/mentions")
    public Result<?> getMyMentions(@PathVariable Long groupId,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(messageService.getMentionMessages(userId, page, size));
    }
}
