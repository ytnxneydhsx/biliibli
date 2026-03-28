package com.bilibili.im.contact.service;

import com.bilibili.im.contact.model.entity.ContactRelationDO;

public interface ContactRelationQueryService {

    ContactRelationDO getRelation(Long ownerUserId, Long targetUserId);

    ContactRelationDO getReceiverViewRelation(Long senderId, Long receiverId);
}
