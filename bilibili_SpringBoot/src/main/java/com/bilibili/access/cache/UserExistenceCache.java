package com.bilibili.access.cache;

public interface UserExistenceCache {

    boolean exists(Long userId);
}
