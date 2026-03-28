package com.bilibili.im.contact.service.impl;

import com.bilibili.im.contact.mapper.ContactRelationMapper;
import com.bilibili.im.contact.model.entity.ContactRelationDO;
import com.bilibili.im.contact.service.ContactRelationQueryService;
import org.springframework.stereotype.Service;

@Service
public class ContactRelationQueryServiceImpl implements ContactRelationQueryService {

    private final ContactRelationMapper contactRelationMapper;

    public ContactRelationQueryServiceImpl(ContactRelationMapper contactRelationMapper) {
        this.contactRelationMapper = contactRelationMapper;
    }

    @Override
    public ContactRelationDO getRelation(Long ownerUserId, Long targetUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId is invalid");
        }
        if (targetUserId == null || targetUserId <= 0) {
            throw new IllegalArgumentException("targetUserId is invalid");
        }
        return contactRelationMapper.selectByUserIdAndTargetUserId(ownerUserId, targetUserId);
    }

    @Override
    public ContactRelationDO getReceiverViewRelation(Long senderId, Long receiverId) {
        if (senderId == null || senderId <= 0) {
            throw new IllegalArgumentException("senderId is invalid");
        }
        if (receiverId == null || receiverId <= 0) {
            throw new IllegalArgumentException("receiverId is invalid");
        }
        return getRelation(receiverId, senderId);
    }
}
