package com.bilibili.im.domain.impl;

import com.bilibili.im.contact.model.entity.ContactRelationDO;
import com.bilibili.im.contact.service.ContactRelationQueryService;
import com.bilibili.im.domain.MessagePermissionDomainService;
import com.bilibili.im.privacy.model.enum.PrivateMessagePolicy;
import com.bilibili.im.privacy.service.UserPrivacyService;
import org.springframework.stereotype.Service;

@Service
public class MessagePermissionDomainServiceImpl implements MessagePermissionDomainService {

    private final UserPrivacyService userPrivacyService;
    private final ContactRelationQueryService contactRelationQueryService;

    public MessagePermissionDomainServiceImpl(UserPrivacyService userPrivacyService,
                                              ContactRelationQueryService contactRelationQueryService) {
        this.userPrivacyService = userPrivacyService;
        this.contactRelationQueryService = contactRelationQueryService;
    }

    @Override
    public void validateCanSendMessage(Long senderId, Long receiverId) {
        if (senderId == null || senderId <= 0) {
            throw new IllegalArgumentException("senderId is invalid");
        }
        if (receiverId == null || receiverId <= 0) {
            throw new IllegalArgumentException("receiverId is invalid");
        }
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("cannot send message to self");
        }

        ContactRelationDO receiverViewRelation = contactRelationQueryService.getReceiverViewRelation(senderId, receiverId);
        if (receiverViewRelation != null && isTrue(receiverViewRelation.getIsBlocked())) {
            throw new IllegalArgumentException("receiver has blocked sender");
        }

        PrivateMessagePolicy policy = userPrivacyService.getPrivateMessagePolicy(receiverId);
        switch (policy) {
            case ALLOW_ALL:
                return;
            case STRANGER_FIRST_MESSAGE_ONLY:
                // TODO: first-message-only rule is not enforced yet.
                // Current behavior temporarily degrades to allow.
                return;
            case CONTACT_ONLY:
                if (receiverViewRelation != null && isTrue(receiverViewRelation.getIsContact())) {
                    return;
                }
                throw new IllegalArgumentException("receiver only accepts messages from contacts");
            case DENY_ALL:
                throw new IllegalArgumentException("receiver does not accept private messages");
            default:
                throw new IllegalArgumentException("private message policy is invalid");
        }
    }

    private static boolean isTrue(Integer value) {
        return value != null && value == 1;
    }
}
