package com.bilibili.im.contact.cache.impl;

import com.bilibili.im.contact.cache.ContactRelationCacheKeys;
import com.bilibili.im.contact.cache.ContactRelationCacheNames;
import com.bilibili.im.contact.cache.ContactRelationSnapshotCache;
import com.bilibili.im.contact.mapper.ContactRelationMapper;
import com.bilibili.im.contact.model.cache.ContactRelationSnapshot;
import com.bilibili.im.contact.model.entity.ContactRelationDO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class SpringContactRelationSnapshotCache implements ContactRelationSnapshotCache {

    private static final int TRUE_VALUE = 1;

    private final ContactRelationMapper contactRelationMapper;

    public SpringContactRelationSnapshotCache(ContactRelationMapper contactRelationMapper) {
        this.contactRelationMapper = contactRelationMapper;
    }

    @Override
    @Cacheable(
            cacheNames = ContactRelationCacheNames.CONTACT_RELATION_SNAPSHOT,
            key = "T(com.bilibili.im.contact.cache.ContactRelationCacheKeys).relationKey(#p0, #p1)"
    )
    public ContactRelationSnapshot get(Long ownerUserId, Long targetUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (targetUserId == null || targetUserId <= 0) {
            throw new IllegalArgumentException("targetUserId is invalid");
        }

        ContactRelationDO relation = contactRelationMapper.selectByUserIdAndTargetUserId(ownerUserId, targetUserId);
        if (relation == null) {
            return ContactRelationSnapshot.notFound(ownerUserId, targetUserId);
        }

        return new ContactRelationSnapshot(
                ownerUserId,
                targetUserId,
                true,
                isTrue(relation.getIsContact()),
                isTrue(relation.getIsDmContact()),
                isTrue(relation.getIsBlocked()),
                isTrue(relation.getIsMuted())
        );
    }

    private boolean isTrue(Integer value) {
        return value != null && value == TRUE_VALUE;
    }
}
