package com.bilibili.access.model.command;

import com.bilibili.access.model.enums.UserAccessRestrictionType;
import lombok.Data;

@Data
public class ApplyVideoAccessRestrictionCommand {

    private Long userId;

    private UserAccessRestrictionType restrictionType;

    private Long operatorId;

    private String reason;
}
