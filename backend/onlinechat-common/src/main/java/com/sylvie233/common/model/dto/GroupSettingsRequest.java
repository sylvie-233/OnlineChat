package com.sylvie233.common.model.dto;

import lombok.Data;

/**
 * 群设置更新请求
 */
@Data
public class GroupSettingsRequest {
    private String groupName;
    private String avatar;
    private String description;
    private Integer joinType;
    private Integer maxMembers;
    private Integer isMutedAll;
}
