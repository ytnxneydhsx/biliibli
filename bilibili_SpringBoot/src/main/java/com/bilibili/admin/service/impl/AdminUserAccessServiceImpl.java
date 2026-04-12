package com.bilibili.admin.service.impl;

import com.bilibili.admin.mapper.AdminUserAccessMapper;
import com.bilibili.admin.service.AdminUserAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserAccessServiceImpl implements AdminUserAccessService {

    private final AdminUserAccessMapper adminUserAccessMapper;

    public AdminUserAccessServiceImpl(AdminUserAccessMapper adminUserAccessMapper) {
        this.adminUserAccessMapper = adminUserAccessMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void banVideoBusiness(Long userId, Long operatorId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        if (operatorId == null || operatorId <= 0) {
            throw new IllegalArgumentException("operatorId is invalid");
        }

        int rows = adminUserAccessMapper.upsertVideoBusinessBanned(userId);
        if (rows <= 0) {
            throw new RuntimeException("ban video business failed");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbanVideoBusiness(Long userId, Long operatorId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        if (operatorId == null || operatorId <= 0) {
            throw new IllegalArgumentException("operatorId is invalid");
        }

        int rows = adminUserAccessMapper.upsertVideoBusinessEnabled(userId);
        if (rows <= 0) {
            throw new RuntimeException("unban video business failed");
        }
    }
}
