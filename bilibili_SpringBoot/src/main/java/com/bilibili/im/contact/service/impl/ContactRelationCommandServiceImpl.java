package com.bilibili.im.contact.service.impl;

import com.bilibili.im.contact.cache.ContactRelationCacheKeys;
import com.bilibili.im.contact.cache.ContactRelationCacheNames;
import com.bilibili.im.contact.cache.ContactRelationSnapshotCache;
import com.bilibili.im.contact.mapper.ContactRelationMapper;
import com.bilibili.im.contact.model.cache.ContactRelationSnapshot;
import com.bilibili.im.contact.service.ContactRelationCommandService;
import com.bilibili.im.metrics.ImDbOperationMetrics;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class ContactRelationCommandServiceImpl implements ContactRelationCommandService {

    private final ContactRelationMapper contactRelationMapper;
    private final ContactRelationSnapshotCache contactRelationSnapshotCache;
    private final ImDbOperationMetrics imDbOperationMetrics;

    public ContactRelationCommandServiceImpl(ContactRelationMapper contactRelationMapper,
                                             ContactRelationSnapshotCache contactRelationSnapshotCache,
                                             ImDbOperationMetrics imDbOperationMetrics) {
        this.contactRelationMapper = contactRelationMapper;
        this.contactRelationSnapshotCache = contactRelationSnapshotCache;
        this.imDbOperationMetrics = imDbOperationMetrics;
    }

    @Override
    @CacheEvict(
            cacheNames = ContactRelationCacheNames.CONTACT_RELATION_SNAPSHOT,
            key = "T(com.bilibili.im.contact.cache.ContactRelationCacheKeys).relationKey(#p0, #p1)"
    )
    public void markDmContact(Long ownerUserId, Long targetUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (targetUserId == null || targetUserId <= 0) {
            throw new IllegalArgumentException("targetUserId is invalid");
        }
        if (ownerUserId.equals(targetUserId)) {
            return;
        }

        ContactRelationSnapshot snapshot = contactRelationSnapshotCache.get(ownerUserId, targetUserId);
        if (snapshot.exists() && snapshot.dmContact()) {
            return;
        }

        int rows = imDbOperationMetrics.record(
                "contact_relation_upsert",
                () -> contactRelationMapper.upsertDmContact(ownerUserId, targetUserId)
        );
        if (rows <= 0) {
            throw new IllegalStateException("mark dm contact failed");
        }
    }
}
