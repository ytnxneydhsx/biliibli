package com.bilibili.im.contact.cache;

public interface ContactRelationDmContactCacheService {

    boolean hasDmContact(Long ownerUserId, Long targetUserId);

    void markDmContact(Long ownerUserId, Long targetUserId);
}
