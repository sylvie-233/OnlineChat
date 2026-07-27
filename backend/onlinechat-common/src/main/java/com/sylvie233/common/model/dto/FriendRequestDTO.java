package com.sylvie233.common.model.dto;

import lombok.Data;

/**
 * 好友申请请求
 */
@Data
public class FriendRequestDTO {
    private Long toUserId;
    private String verifyMessage;
    private String source;
}
