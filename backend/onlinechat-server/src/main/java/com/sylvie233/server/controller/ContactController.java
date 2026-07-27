package com.sylvie233.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sylvie233.common.model.dto.FriendRequestDTO;
import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.Contact;
import com.sylvie233.repository.entity.ContactGroup;
import com.sylvie233.repository.entity.FriendRequest;
import com.sylvie233.repository.entity.Blocklist;
import com.sylvie233.service.contact.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 联系人接口 — 好友列表、分组管理、申请处理、黑名单
 */
@Tag(name = "联系人")
@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @Operation(summary = "获取好友分组列表")
    @GetMapping("/groups")
    public Result<List<ContactGroup>> getGroups() {
        return Result.ok(contactService.getGroups(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "创建好友分组")
    @PostMapping("/groups")
    public Result<ContactGroup> createGroup(@RequestBody Map<String, String> body) {
        return Result.ok(contactService.createGroup(
                StpUtil.getLoginIdAsLong(), body.get("groupName")));
    }

    @Operation(summary = "重命名分组")
    @PutMapping("/groups/{groupId}")
    public Result<?> renameGroup(@PathVariable Long groupId,
                                  @RequestBody Map<String, String> body) {
        contactService.renameGroup(groupId, StpUtil.getLoginIdAsLong(), body.get("groupName"));
        return Result.ok();
    }

    @Operation(summary = "删除分组")
    @DeleteMapping("/groups/{groupId}")
    public Result<?> deleteGroup(@PathVariable Long groupId) {
        contactService.deleteGroup(groupId, StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    @Operation(summary = "获取好友列表")
    @GetMapping("/list")
    public Result<List<Contact>> getContacts() {
        return Result.ok(contactService.getContacts(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "删除好友")
    @DeleteMapping("/{contactUserId}")
    public Result<?> deleteContact(@PathVariable Long contactUserId) {
        contactService.deleteContact(StpUtil.getLoginIdAsLong(), contactUserId);
        return Result.ok();
    }

    @Operation(summary = "修改备注")
    @PutMapping("/remark")
    public Result<?> updateRemark(@RequestBody Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long contactUserId = Long.valueOf(body.get("contactUserId").toString());
        contactService.updateRemark(userId, contactUserId, (String) body.get("remark"));
        return Result.ok();
    }

    @Operation(summary = "星标/取消星标")
    @PutMapping("/star")
    public Result<?> toggleStar(@RequestBody Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long contactUserId = Long.valueOf(body.get("contactUserId").toString());
        boolean starred = (boolean) body.getOrDefault("starred", true);
        contactService.toggleStar(userId, contactUserId, starred);
        return Result.ok();
    }

    @Operation(summary = "移动好友到分组")
    @PutMapping("/move-group")
    public Result<?> moveToGroup(@RequestBody Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long contactUserId = Long.valueOf(body.get("contactUserId").toString());
        Long groupId = Long.valueOf(body.get("groupId").toString());
        contactService.moveToGroup(userId, contactUserId, groupId);
        return Result.ok();
    }

    @Operation(summary = "发送好友申请")
    @PostMapping("/request")
    public Result<FriendRequest> sendRequest(@RequestBody FriendRequestDTO req) {
        return Result.ok(contactService.sendRequest(
                StpUtil.getLoginIdAsLong(), req.getToUserId(),
                req.getVerifyMessage(), req.getSource()));
    }

    @Operation(summary = "获取收到的好友申请")
    @GetMapping("/requests")
    public Result<List<FriendRequest>> getRequests() {
        return Result.ok(contactService.getPendingRequests(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "处理好友申请")
    @PutMapping("/request/{requestId}")
    public Result<?> handleRequest(@PathVariable Long requestId,
                                    @RequestBody Map<String, Boolean> body) {
        contactService.handleRequest(requestId, StpUtil.getLoginIdAsLong(),
                body.getOrDefault("agree", false));
        return Result.ok();
    }

    @Operation(summary = "拉黑用户")
    @PostMapping("/block/{blockedUserId}")
    public Result<?> blockUser(@PathVariable Long blockedUserId,
                                @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        contactService.blockUser(StpUtil.getLoginIdAsLong(), blockedUserId, reason);
        return Result.ok();
    }

    @Operation(summary = "取消拉黑")
    @DeleteMapping("/block/{blockedUserId}")
    public Result<?> unblockUser(@PathVariable Long blockedUserId) {
        contactService.unblockUser(StpUtil.getLoginIdAsLong(), blockedUserId);
        return Result.ok();
    }

    @Operation(summary = "获取黑名单列表")
    @GetMapping("/blocks")
    public Result<List<Blocklist>> getBlocks() {
        return Result.ok(contactService.getBlocklist(StpUtil.getLoginIdAsLong()));
    }
}
