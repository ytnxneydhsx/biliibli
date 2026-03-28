package com.bilibili.access.service;

import com.bilibili.access.model.command.ApplyImAccessRestrictionCommand;
import com.bilibili.access.model.command.ApplyVideoAccessRestrictionCommand;

public interface UserAccessAdminService {

    void applyVideoAccessRestriction(ApplyVideoAccessRestrictionCommand command);

    void applyImAccessRestriction(ApplyImAccessRestrictionCommand command);
}
