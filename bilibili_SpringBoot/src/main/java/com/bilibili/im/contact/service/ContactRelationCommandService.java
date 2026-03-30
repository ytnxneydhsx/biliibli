package com.bilibili.im.contact.service;

public interface ContactRelationCommandService {

    void markDmContact(Long ownerUserId, Long targetUserId);
}
