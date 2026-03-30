package com.bilibili.im.app.impl;

import com.bilibili.access.service.UserAccessService;
import com.bilibili.im.app.ImApplicationService;
import com.bilibili.im.common.time.ImTimeService;
import com.bilibili.im.conversation.service.ChatConversationService;
import com.bilibili.im.domain.MessagePermissionDomainService;
import com.bilibili.im.message.model.command.SendMessageCommand;
import com.bilibili.im.message.model.dto.MessageContentDTO;
import com.bilibili.im.message.model.enums.MessageStatus;
import com.bilibili.im.message.model.enums.MessageType;
import com.bilibili.im.message.model.vo.SendMessageVO;
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
    private final ImTimeService imTimeService;
    private final ImMessageProducer imMessageProducer;
    private final IpLocationService ipLocationService;

    public ImApplicationServiceImpl(UserAccessService userAccessService,
                                    MessagePermissionDomainService messagePermissionDomainService,
                                    ChatConversationService chatConversationService,
                                    ImTimeService imTimeService,
                                    ImMessageProducer imMessageProducer,
                                    IpLocationService ipLocationService) {
        this.userAccessService = userAccessService;
        this.messagePermissionDomainService = messagePermissionDomainService;
        this.chatConversationService = chatConversationService;
        this.imTimeService = imTimeService;
        this.imMessageProducer = imMessageProducer;
        this.ipLocationService = ipLocationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SendMessageVO acceptMessage(Long senderId, String clientIp, SendMessageCommand command) {
        if (senderId == null || senderId <= 0) {
            throw new IllegalArgumentException("senderId is invalid");
        }
        if (command == null) {
            throw new IllegalArgumentException("command is invalid");
        }
        userAccessService.validateCanSendImMessage(senderId);
        messagePermissionDomainService.validateCanSendMessage(senderId, command.getReceiverId());
        validateMessageContent(command.getMessageType(), command.getContent());

        String conversationId = chatConversationService.resolveSingleConversationId(senderId, command.getReceiverId());
        LocalDateTime sendTime = imTimeService.now();
        String senderLocation = ipLocationService.resolveLocation(clientIp);
        ImMessageDispatchEvent dispatchEvent = buildDispatchEvent(conversationId, senderId, command, sendTime, senderLocation);
        imMessageProducer.publish(dispatchEvent);

        SendMessageVO sendMessageVO = new SendMessageVO();
        sendMessageVO.setConversationId(conversationId);
        sendMessageVO.setMessageType(command.getMessageType());
        sendMessageVO.setContent(command.getContent());
        sendMessageVO.setSenderLocation(senderLocation);
        sendMessageVO.setSendTime(sendTime);
        sendMessageVO.setStatus(MessageStatus.ACCEPTED.getCode());
        return sendMessageVO;
    }

    private static ImMessageDispatchEvent buildDispatchEvent(String conversationId,
                                                             Long senderId,
                                                             SendMessageCommand command,
                                                             LocalDateTime sendTime,
                                                             String senderLocation) {
        ImMessageDispatchEvent dispatchEvent = new ImMessageDispatchEvent();
        dispatchEvent.setConversationId(conversationId);
        dispatchEvent.setSenderId(senderId);
        dispatchEvent.setReceiverId(command.getReceiverId());
        dispatchEvent.setMessageType(command.getMessageType());
        dispatchEvent.setContent(command.getContent());
        dispatchEvent.setSendTime(sendTime);
        dispatchEvent.setClientMessageId(command.getClientMessageId());
        dispatchEvent.setSenderLocation(senderLocation);
        return dispatchEvent;
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
}
