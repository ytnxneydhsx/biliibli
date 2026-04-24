package com.bilibili.admin.service.impl;

import com.bilibili.admin.mapper.AdminUserAccessMapper;
import com.bilibili.admin.mapper.AdminUserMapper;
import com.bilibili.admin.model.vo.AdminUserVO;
import com.bilibili.admin.service.AdminUserAccessService;
import com.bilibili.access.cache.AccessCacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserAccessServiceImpl implements AdminUserAccessService {

    private final AdminUserAccessMapper adminUserAccessMapper;
    private final AdminUserMapper adminUserMapper;

    public AdminUserAccessServiceImpl(AdminUserAccessMapper adminUserAccessMapper,
                                      AdminUserMapper adminUserMapper) {
        this.adminUserAccessMapper = adminUserAccessMapper;
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    @CacheEvict(cacheNames = AccessCacheNames.USER_ACCESS_SNAPSHOT, key = "#p0")
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO banVideoBusiness(Long userId, Long operatorId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        if (operatorId == null || operatorId <= 0) {
            throw new IllegalArgumentException("operatorId is invalid");
        }
        validateUserExists(userId);

        int rows = adminUserAccessMapper.upsertVideoBusinessBanned(userId);
        if (rows <= 0) {
            throw new RuntimeException("ban video business failed");
        }
        return adminUserMapper.selectAdminUserById(userId);
    }

    @Override
    @CacheEvict(cacheNames = AccessCacheNames.USER_ACCESS_SNAPSHOT, key = "#p0")
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO unbanVideoBusiness(Long userId, Long operatorId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        if (operatorId == null || operatorId <= 0) {
            throw new IllegalArgumentException("operatorId is invalid");
        }
        validateUserExists(userId);

        int rows = adminUserAccessMapper.upsertVideoBusinessEnabled(userId);
        if (rows <= 0) {
            throw new RuntimeException("unban video business failed");
        }
        return adminUserMapper.selectAdminUserById(userId);
    }

    private void validateUserExists(Long userId) {
        if (adminUserMapper.selectAdminUserById(userId) == null) {
            throw new IllegalArgumentException("user not found");
        }
    }
}
