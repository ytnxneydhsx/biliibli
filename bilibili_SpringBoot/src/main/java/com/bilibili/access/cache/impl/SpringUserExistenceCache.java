package com.bilibili.access.cache.impl;

import com.bilibili.access.cache.AccessCacheNames;
import com.bilibili.access.cache.UserExistenceCache;
import com.bilibili.user.mapper.UserMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class SpringUserExistenceCache implements UserExistenceCache {

    private final UserMapper userMapper;

    public SpringUserExistenceCache(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Cacheable(cacheNames = AccessCacheNames.USER_EXISTS, key = "#p0")
    public boolean exists(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        return userMapper.selectById(userId) != null;
    }
}
