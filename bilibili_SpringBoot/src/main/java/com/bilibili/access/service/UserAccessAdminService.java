package com.bilibili.access.service;

import com.bilibili.access.model.command.ApplyImAccessRestrictionCommand;

public interface UserAccessAdminService {

    void applyImAccessRestriction(ApplyImAccessRestrictionCommand command);
}
