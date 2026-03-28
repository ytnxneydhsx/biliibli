package com.bilibili.access.service.impl;

import com.bilibili.access.mapper.UserAccessMapper;
import com.bilibili.access.model.command.ApplyImAccessRestrictionCommand;
import com.bilibili.access.model.command.ApplyVideoAccessRestrictionCommand;
import com.bilibili.access.model.enums.UserAccessRestrictionType;
import com.bilibili.access.service.UserAccessAdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccessAdminServiceImpl implements UserAccessAdminService {

    private final UserAccessMapper userAccessMapper;

    public UserAccessAdminServiceImpl(UserAccessMapper userAccessMapper) {
        this.userAccessMapper = userAccessMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyVideoAccessRestriction(ApplyVideoAccessRestrictionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is null");
        }
        if (command.getUserId() == null || command.getUserId() <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        if (command.getRestrictionType() == null) {
            throw new IllegalArgumentException("restrictionType is invalid");
        }

        if (UserAccessRestrictionType.VIDEO_BUSINESS_BANNED == command.getRestrictionType()) {
            int rows = userAccessMapper.upsertVideoBusinessBanned(command.getUserId());
            if (rows <= 0) {
                throw new RuntimeException("apply video access restriction failed");
            }
            return;
        }

        throw new IllegalArgumentException("unsupported restrictionType");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyImAccessRestriction(ApplyImAccessRestrictionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is null");
        }
        if (command.getUserId() == null || command.getUserId() <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        if (command.getRestrictionType() == null) {
            throw new IllegalArgumentException("restrictionType is invalid");
        }

        if (UserAccessRestrictionType.IM_MESSAGE_SEND_BANNED == command.getRestrictionType()) {
            int rows = userAccessMapper.upsertImMessageSendBanned(command.getUserId());
            if (rows <= 0) {
                throw new RuntimeException("apply im access restriction failed");
            }
            return;
        }

        throw new IllegalArgumentException("unsupported restrictionType");
    }
}
