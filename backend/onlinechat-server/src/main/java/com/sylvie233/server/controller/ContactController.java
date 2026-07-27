package com.sylvie233.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.Contact;
import com.sylvie233.repository.entity.ContactGroup;
import com.sylvie233.repository.entity.FriendRequest;
import com.sylvie233.service.contact.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 联系人接口 — 好友列表、申请处理、黑名单
 */
@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    // ==================== 好友分组 ====================

    @GetMapping("/groups")
    public Result<List<ContactGroup>> getGroups() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(contactService.getGroups(userId));
    }

    @PostMapping("/groups")
    public Result<ContactGroup> createGroup(@RequestBody Map<String, String> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(contactService.createGroup(userId, body.get("groupName")));
    }

    // ==================== 好友列表 ====================

    @GetMapping("/list")
    public Result<List<Contact>> getContacts() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(contactService.getContacts(userId));
    }

    @DeleteMapping("/{contactUserId}")
    public Result<?> deleteContact(@PathVariable Long contactUserId) {
        Long userId = StpUtil.getLoginIdAsLong();
        contactService.deleteContact(userId, contactUserId);
        return Result.ok();
    }

    @PutMapping("/remark")
    public Result<?> updateRemark(@RequestBody Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long contactUserId = Long.valueOf(body.get("contactUserId").toString());
        String remark = (String) body.get("remark");
        contactService.updateRemark(userId, contactUserId, remark);
        return Result.ok();
    }

    // ==================== 好友申请 ====================

    @PostMapping("/request")
    public Result<FriendRequest> sendRequest(@RequestBody Map<String, String> body) {
        Long fromUserId = StpUtil.getLoginIdAsLong();
        Long toUserId = Long.valueOf(body.get("toUserId"));
        String verifyMessage = body.getOrDefault("verifyMessage", "");
        return Result.ok(contactService.sendRequest(fromUserId, toUserId, verifyMessage));
    }

    @PutMapping("/request/{requestId}")
    public Result<?> handleRequest(@PathVariable Long requestId,
                                    @RequestBody Map<String, Boolean> body) {
        Long handlerId = StpUtil.getLoginIdAsLong();
        boolean agree = body.getOrDefault("agree", false);
        contactService.handleRequest(requestId, handlerId, agree);
        return Result.ok();
    }

    // ==================== 黑名单 ====================

    @PostMapping("/block/{blockedUserId}")
    public Result<?> blockUser(@PathVariable Long blockedUserId) {
        Long userId = StpUtil.getLoginIdAsLong();
        contactService.blockUser(userId, blockedUserId);
        return Result.ok();
    }

    @DeleteMapping("/block/{blockedUserId}")
    public Result<?> unblockUser(@PathVariable Long blockedUserId) {
        Long userId = StpUtil.getLoginIdAsLong();
        contactService.unblockUser(userId, blockedUserId);
        return Result.ok();
    }
}
