package com.bilibili.im.group.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;

@Component
public class GroupPermissionCacheEvictor {

    private final CacheManager cacheManager;

    public GroupPermissionCacheEvictor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictGroup(Long groupId) {
        if (groupId == null || groupId <= 0) {
            return;
        }
        runAfterCommitOrNow(() -> getRequiredCache(GroupPermissionCacheNames.GROUP_SNAPSHOT).evict(groupId));
    }

    public void evictGroupMember(Long groupId, Long userId) {
        if (!isValidGroupId(groupId) || !isValidUserId(userId)) {
            return;
        }
        String key = groupMemberKey(groupId, userId);
        runAfterCommitOrNow(() -> getRequiredCache(GroupPermissionCacheNames.GROUP_MEMBER_SNAPSHOT).evict(key));
    }

    public void evictGroupMembers(Long groupId, Collection<Long> userIds) {
        if (!isValidGroupId(groupId) || userIds == null || userIds.isEmpty()) {
            return;
        }
        runAfterCommitOrNow(() -> {
            Cache cache = getRequiredCache(GroupPermissionCacheNames.GROUP_MEMBER_SNAPSHOT);
            for (Long userId : userIds) {
                if (isValidUserId(userId)) {
                    cache.evict(groupMemberKey(groupId, userId));
                }
            }
        });
    }

    public static String groupMemberKey(Long groupId, Long userId) {
        return groupId + ":" + userId;
    }

    private void runAfterCommitOrNow(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }

    private Cache getRequiredCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalStateException("cache not configured: " + cacheName);
        }
        return cache;
    }

    private boolean isValidGroupId(Long groupId) {
        return groupId != null && groupId > 0;
    }

    private boolean isValidUserId(Long userId) {
        return userId != null && userId > 0;
    }
}
