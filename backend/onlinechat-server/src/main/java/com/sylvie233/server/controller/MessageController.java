package com.sylvie233.server.controller;

import com.sylvie233.common.enums.ConversationType;
import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.Message;
import com.sylvie233.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 消息接口（历史消息查询、离线消息拉取）
 */
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 拉取会话最新消息
     */
    @GetMapping("/latest")
    public Result<List<Message>> getLatest(@RequestParam Long conversationId,
                                            @RequestParam(defaultValue = "0") int type,
                                            @RequestParam(defaultValue = "20") int limit) {
        ConversationType convType;
        try {
            convType = ConversationType.values()[type];
        } catch (IndexOutOfBoundsException e) {
            return Result.fail("无效的会话类型");
        }
        List<Message> messages = messageService.getLatestMessages(conversationId, convType, limit);
        return Result.ok(messages);
    }

    /**
     * 拉取历史消息（翻页）
     */
    @GetMapping("/history")
    public Result<List<Message>> getHistory(@RequestParam Long conversationId,
                                             @RequestParam(defaultValue = "0") int type,
                                             @RequestParam Long cursorSeq,
                                             @RequestParam(defaultValue = "20") int limit) {
        ConversationType convType;
        try {
            convType = ConversationType.values()[type];
        } catch (IndexOutOfBoundsException e) {
            return Result.fail("无效的会话类型");
        }
        List<Message> messages = messageService.getHistoryMessages(conversationId, convType, cursorSeq, limit);
        return Result.ok(messages);
    }
}
