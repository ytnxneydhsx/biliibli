package com.bilibili.im.contact.service.impl;

import com.bilibili.im.contact.mapper.ContactRelationMapper;
import com.bilibili.im.contact.service.ContactRelationCommandService;
import org.springframework.stereotype.Service;

@Service
public class ContactRelationCommandServiceImpl implements ContactRelationCommandService {

    private final ContactRelationMapper contactRelationMapper;

    public ContactRelationCommandServiceImpl(ContactRelationMapper contactRelationMapper) {
        this.contactRelationMapper = contactRelationMapper;
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

        int rows = contactRelationMapper.upsertDmContact(ownerUserId, targetUserId);
        if (rows <= 0) {
            throw new IllegalStateException("mark dm contact failed");
        }
    }
}
