package com.bilibili.im.contact.service.impl;

import com.bilibili.im.contact.cache.ContactRelationDmContactCacheService;
import com.bilibili.im.contact.mapper.ContactRelationMapper;
import com.bilibili.im.contact.service.ContactRelationCommandService;
import com.bilibili.im.metrics.ImDbOperationMetrics;
import org.springframework.stereotype.Service;

@Service
public class ContactRelationCommandServiceImpl implements ContactRelationCommandService {

    private final ContactRelationMapper contactRelationMapper;
    private final ContactRelationDmContactCacheService dmContactCacheService;
    private final ImDbOperationMetrics imDbOperationMetrics;

    public ContactRelationCommandServiceImpl(ContactRelationMapper contactRelationMapper,
                                             ContactRelationDmContactCacheService dmContactCacheService,
                                             ImDbOperationMetrics imDbOperationMetrics) {
        this.contactRelationMapper = contactRelationMapper;
        this.dmContactCacheService = dmContactCacheService;
        this.imDbOperationMetrics = imDbOperationMetrics;
    }

    @Override
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

        if (dmContactCacheService.hasDmContact(ownerUserId, targetUserId)) {
            return;
        }

        int rows = imDbOperationMetrics.record(
                "contact_relation_upsert",
                () -> contactRelationMapper.upsertDmContact(ownerUserId, targetUserId)
        );
        if (rows <= 0) {
            throw new IllegalStateException("mark dm contact failed");
        }

        dmContactCacheService.markDmContact(ownerUserId, targetUserId);
    }
}
