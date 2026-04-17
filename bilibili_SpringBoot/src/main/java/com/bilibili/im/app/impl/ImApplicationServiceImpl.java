package com.bilibili.im.app.impl;

import com.bilibili.access.service.UserAccessService;
import com.bilibili.im.app.ImApplicationService;
import com.bilibili.im.common.id.MessageIdGenerator;
import com.bilibili.im.common.time.ImTimeService;
import com.bilibili.im.conversation.model.enums.ConversationType;
import com.bilibili.im.conversation.service.ChatConversationService;
import com.bilibili.im.conversation.service.ChatGroupConversationService;
import com.bilibili.im.domain.MessagePermissionDomainService;
import com.bilibili.im.group.permission.GroupPermissionService;
import com.bilibili.im.message.model.command.SendMessageCommand;
import com.bilibili.im.message.model.dto.MessageContentDTO;
import com.bilibili.im.message.model.enums.MessageStatus;
import com.bilibili.im.message.model.enums.MessageType;
import com.bilibili.im.message.model.vo.SendMessageVO;
import com.bilibili.im.metrics.ImSendObservation;
import com.bilibili.im.moderation.service.SensitiveWordTrieService;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import com.bilibili.im.mq.producer.ImMessageProducer;
import com.bilibili.location.service.IpLocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ImApplicationServiceImpl implements ImApplicationService {

    private final UserAccessService userAccessService;
    private final MessagePermissionDomainService messagePermissionDomainService;
    private final ChatConversationService chatConversationService;
    private final ChatGroupConversationService chatGroupConversationService;
    private final GroupPermissionService groupPermissionService;
    private final ImTimeService imTimeService;
    private final ImMessageProducer imMessageProducer;
    private final IpLocationService ipLocationService;
    private final MessageIdGenerator messageIdGenerator;
    private final SensitiveWordTrieService sensitiveWordTrieService;
    private final ImSendObservation imSendObservation;

    public ImApplicationServiceImpl(UserAccessService userAccessService,
                                    MessagePermissionDomainService messagePermissionDomainService,
                                    ChatConversationService chatConversationService,
                                    ChatGroupConversationService chatGroupConversationService,
                                    GroupPermissionService groupPermissionService,
                                    ImTimeService imTimeService,
                                    ImMessageProducer imMessageProducer,
                                    IpLocationService ipLocationService,
                                    MessageIdGenerator messageIdGenerator,
                                    SensitiveWordTrieService sensitiveWordTrieService,
                                    ImSendObservation imSendObservation) {
        this.userAccessService = userAccessService;
        this.messagePermissionDomainService = messagePermissionDomainService;
        this.chatConversationService = chatConversationService;
        this.chatGroupConversationService = chatGroupConversationService;
        this.groupPermissionService = groupPermissionService;
        this.imTimeService = imTimeService;
        this.imMessageProducer = imMessageProducer;
        this.ipLocationService = ipLocationService;
        this.messageIdGenerator = messageIdGenerator;
        this.sensitiveWordTrieService = sensitiveWordTrieService;
        this.imSendObservation = imSendObservation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SendMessageVO acceptMessage(Long senderId, String clientIp, SendMessageCommand command) {
        Long clientMessageId = command == null ? null : command.getClientMessageId();
        try (ImSendObservation.Context observation = imSendObservation.startAccept(senderId, clientMessageId)) {
            if (senderId == null || senderId <= 0) {
                throw new IllegalArgumentException("senderId is invalid");
            }
            if (command == null) {
                throw new IllegalArgumentException("command is invalid");
            }
            Integer conversationType = normalizeConversationType(command.getConversationType());
            command.setConversationType(conversationType);

            observation.observeValidation(() -> {
                userAccessService.validateCanSendImMessage(senderId);
                validateMessageContent(command.getMessageType(), command.getContent());
                validateSensitiveWord(command.getContent());
            });

            String conversationId = observation.observeConversation(() ->
                    resolveConversationId(senderId, command.getReceiverId(), conversationType)
            );

            LocalDateTime sendTime = imTimeService.now();
            String senderLocation = observation.observeLocation(() ->
                    ipLocationService.resolveLocation(clientIp)
            );

            long serverMessageId = messageIdGenerator.nextId();
            ImMessageDispatchEvent dispatchEvent = buildDispatchEvent(
                    conversationId,
                    senderId,
                    command,
                    sendTime,
                    senderLocation,
                    serverMessageId
            );

            observation.observePublish(() -> imMessageProducer.publish(dispatchEvent));

            SendMessageVO sendMessageVO = new SendMessageVO();
            sendMessageVO.setConversationId(conversationId);
            sendMessageVO.setClientMessageId(command.getClientMessageId());
            sendMessageVO.setMessageType(command.getMessageType());
            sendMessageVO.setContent(command.getContent());
            sendMessageVO.setSenderLocation(senderLocation);
            sendMessageVO.setSendTime(sendTime);
            sendMessageVO.setStatus(MessageStatus.ACCEPTED.getCode());
            return sendMessageVO;
        }
    }

    private String resolveConversationId(Long senderId, Long receiverId, Integer conversationType) {
        if (Integer.valueOf(ConversationType.SINGLE.getCode()).equals(conversationType)) {
            messagePermissionDomainService.validateCanSendMessage(senderId, receiverId);
            return chatConversationService.resolveSingleConversationId(senderId, receiverId);
        }

        groupPermissionService.requireActiveGroup(receiverId);
        groupPermissionService.requireActiveMembership(receiverId, senderId);
        return chatGroupConversationService.resolveGroupConversationId(receiverId);
    }

    private static ImMessageDispatchEvent buildDispatchEvent(String conversationId,
                                                             Long senderId,
                                                             SendMessageCommand command,
                                                             LocalDateTime sendTime,
                                                             String senderLocation,
                                                             Long serverMessageId) {
        ImMessageDispatchEvent dispatchEvent = new ImMessageDispatchEvent();
        dispatchEvent.setServerMessageId(serverMessageId);
        dispatchEvent.setConversationId(conversationId);
        dispatchEvent.setConversationType(command.getConversationType());
        dispatchEvent.setSenderId(senderId);
        dispatchEvent.setReceiverId(command.getReceiverId());
        dispatchEvent.setMessageType(command.getMessageType());
        dispatchEvent.setContent(command.getContent());
        dispatchEvent.setSendTime(sendTime);
        dispatchEvent.setClientMessageId(command.getClientMessageId());
        dispatchEvent.setSenderLocation(senderLocation);
        return dispatchEvent;
    }

    private Integer normalizeConversationType(Integer rawConversationType) {
        if (rawConversationType == null) {
            return ConversationType.SINGLE.getCode();
        }
        if (Integer.valueOf(ConversationType.SINGLE.getCode()).equals(rawConversationType)
                || Integer.valueOf(ConversationType.GROUP.getCode()).equals(rawConversationType)) {
            return rawConversationType;
        }
        throw new IllegalArgumentException("conversationType is invalid");
    }

    private void validateMessageContent(Integer messageType, MessageContentDTO content) {
        if (!MessageType.supports(messageType)) {
            throw new IllegalArgumentException("messageType is invalid");
        }
        if (content == null) {
            throw new IllegalArgumentException("content is invalid");
        }

        String text = content.getText() == null ? null : content.getText().trim();
        List<String> imageUrls = content.getImageUrls() == null ? Collections.emptyList() : content.getImageUrls();
        boolean hasText = text != null && !text.isEmpty();
        boolean hasImages = !imageUrls.isEmpty();

        MessageType type = MessageType.fromCode(messageType);
        if (type == MessageType.TEXT && !hasText) {
            throw new IllegalArgumentException("text message content is invalid");
        }
        if (type == MessageType.IMAGE && !hasImages) {
            throw new IllegalArgumentException("image message content is invalid");
        }
        if (type == MessageType.RICH && !hasText && !hasImages) {
            throw new IllegalArgumentException("rich message content is invalid");
        }
    }

    private void validateSensitiveWord(MessageContentDTO content) {
        if (content == null || content.getText() == null || content.getText().isBlank()) {
            return;
        }
        if (sensitiveWordTrieService.containSensitiveWord(content.getText())) {
            throw new IllegalArgumentException("message contains sensitive word");
        }
    }
}
