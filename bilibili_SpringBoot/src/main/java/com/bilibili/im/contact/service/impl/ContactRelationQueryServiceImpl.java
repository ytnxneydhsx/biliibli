package com.bilibili.im.contact.service.impl;

import com.bilibili.im.contact.cache.ContactRelationSnapshotCache;
import com.bilibili.im.contact.model.cache.ContactRelationSnapshot;
import com.bilibili.im.contact.model.entity.ContactRelationDO;
import com.bilibili.im.contact.service.ContactRelationQueryService;
import org.springframework.stereotype.Service;

@Service
public class ContactRelationQueryServiceImpl implements ContactRelationQueryService {

    private static final int TRUE_VALUE = 1;

    private final ContactRelationSnapshotCache contactRelationSnapshotCache;

    public ContactRelationQueryServiceImpl(ContactRelationSnapshotCache contactRelationSnapshotCache) {
        this.contactRelationSnapshotCache = contactRelationSnapshotCache;
    }

    @Override
    public ContactRelationDO getRelation(Long ownerUserId, Long targetUserId) {
        ContactRelationSnapshot snapshot = contactRelationSnapshotCache.get(ownerUserId, targetUserId);
        if (!snapshot.exists()) {
            return null;
        }
        ContactRelationDO relation = new ContactRelationDO();
        relation.setUserId(snapshot.ownerUserId());
        relation.setTargetUserId(snapshot.targetUserId());
        relation.setIsContact(toInt(snapshot.contact()));
        relation.setIsDmContact(toInt(snapshot.dmContact()));
        relation.setIsBlocked(toInt(snapshot.blocked()));
        relation.setIsMuted(toInt(snapshot.muted()));
        return relation;
    }

    private Integer toInt(boolean value) {
        return value ? TRUE_VALUE : 0;
    }
}
