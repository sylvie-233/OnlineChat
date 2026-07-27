package com.sylvie233.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.GroupInfo;
import com.sylvie233.service.group.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 群组接口 — 创建/解散/加群/踢人/设置
 */
@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    /**
     * 创建群
     */
    @PostMapping
    public Result<GroupInfo> createGroup(@RequestBody Map<String, String> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        String groupName = body.get("groupName");
        return Result.ok(groupService.createGroup(userId, groupName));
    }

    /**
     * 获取群信息
     */
    @GetMapping("/{groupId}")
    public Result<GroupInfo> getGroup(@PathVariable Long groupId) {
        return Result.ok(groupService.getById(groupId));
    }

    /**
     * 加入群
     */
    @PostMapping("/{groupId}/join")
    public Result<?> joinGroup(@PathVariable Long groupId) {
        Long userId = StpUtil.getLoginIdAsLong();
        groupService.joinGroup(groupId, userId);
        return Result.ok("加入成功");
    }

    /**
     * 踢出成员
     */
    @DeleteMapping("/{groupId}/member/{userId}")
    public Result<?> kickMember(@PathVariable Long groupId,
                                 @PathVariable Long userId) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        groupService.kickMember(groupId, operatorId, userId);
        return Result.ok();
    }

    /**
     * 解散群
     */
    @DeleteMapping("/{groupId}")
    public Result<?> dismissGroup(@PathVariable Long groupId) {
        Long userId = StpUtil.getLoginIdAsLong();
        GroupInfo group = groupService.getById(groupId);
        if (group == null || !group.getOwnerId().equals(userId)) {
            return Result.fail(403, "只有群主可以解散群");
        }
        group.setStatus(1);
        groupService.updateById(group);
        return Result.ok();
    }
}
