package com.bilibili.im.contact.cache;

import com.bilibili.im.contact.model.cache.ContactRelationSnapshot;

public interface ContactRelationSnapshotCache {

    ContactRelationSnapshot get(Long ownerUserId, Long targetUserId);
}
