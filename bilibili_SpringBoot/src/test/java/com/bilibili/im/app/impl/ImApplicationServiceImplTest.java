package com.bilibili.im.app.impl;

import com.bilibili.access.service.UserAccessService;
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
import com.bilibili.im.moderation.service.SensitiveWordTrieService;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import com.bilibili.im.mq.producer.ImMessageProducer;
import com.bilibili.location.service.IpLocationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImApplicationServiceImplTest {

    @Test
    void shouldAcceptGroupMessageAndPublishDispatchEvent() {
        UserAccessService userAccessService = mock(UserAccessService.class);
        MessagePermissionDomainService messagePermissionDomainService = mock(MessagePermissionDomainService.class);
        ChatConversationService chatConversationService = mock(ChatConversationService.class);
        ChatGroupConversationService chatGroupConversationService = mock(ChatGroupConversationService.class);
        GroupPermissionService groupPermissionService = mock(GroupPermissionService.class);
        ImTimeService imTimeService = mock(ImTimeService.class);
        ImMessageProducer imMessageProducer = mock(ImMessageProducer.class);
        IpLocationService ipLocationService = mock(IpLocationService.class);
        MessageIdGenerator messageIdGenerator = mock(MessageIdGenerator.class);
        SensitiveWordTrieService sensitiveWordTrieService = mock(SensitiveWordTrieService.class);

        ImApplicationServiceImpl service = new ImApplicationServiceImpl(
                userAccessService,
                messagePermissionDomainService,
                chatConversationService,
                chatGroupConversationService,
                groupPermissionService,
                imTimeService,
                imMessageProducer,
                ipLocationService,
                messageIdGenerator,
                sensitiveWordTrieService
        );

        LocalDateTime now = LocalDateTime.of(2026, 4, 7, 12, 30, 0);
        when(imTimeService.now()).thenReturn(now);
        when(ipLocationService.resolveLocation("127.0.0.1")).thenReturn("Shanghai");
        when(messageIdGenerator.nextId()).thenReturn(9527L);
        when(chatGroupConversationService.resolveGroupConversationId(3001L)).thenReturn("g_3001");
        when(sensitiveWordTrieService.containSensitiveWord("hello group")).thenReturn(false);
        doNothing().when(userAccessService).validateCanSendImMessage(1001L);

        SendMessageCommand command = new SendMessageCommand();
        command.setConversationType(ConversationType.GROUP.getCode());
        command.setReceiverId(3001L);
        command.setClientMessageId(7001L);
        command.setMessageType(MessageType.TEXT.getCode());
        MessageContentDTO content = new MessageContentDTO();
        content.setText("hello group");
        command.setContent(content);

        SendMessageVO result = service.acceptMessage(1001L, "127.0.0.1", command);

        verify(groupPermissionService).requireActiveGroup(3001L);
        verify(groupPermissionService).requireActiveMembership(3001L, 1001L);
        verify(messagePermissionDomainService, never()).validateCanSendMessage(anyLong(), anyLong());
        verify(chatConversationService, never()).resolveSingleConversationId(anyLong(), anyLong());

        ArgumentCaptor<ImMessageDispatchEvent> eventCaptor = ArgumentCaptor.forClass(ImMessageDispatchEvent.class);
        verify(imMessageProducer).publish(eventCaptor.capture());
        ImMessageDispatchEvent event = eventCaptor.getValue();
        assertNotNull(event);
        assertEquals("g_3001", event.getConversationId());
        assertEquals(ConversationType.GROUP.getCode(), event.getConversationType());
        assertEquals(1001L, event.getSenderId());
        assertEquals(3001L, event.getReceiverId());
        assertEquals(9527L, event.getServerMessageId());

        assertNotNull(result);
        assertEquals("g_3001", result.getConversationId());
        assertEquals(MessageStatus.ACCEPTED.getCode(), result.getStatus());
        assertEquals(now, result.getSendTime());
        assertEquals("Shanghai", result.getSenderLocation());
    }
}
