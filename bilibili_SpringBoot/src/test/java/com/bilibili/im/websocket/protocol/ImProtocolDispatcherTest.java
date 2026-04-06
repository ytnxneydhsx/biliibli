package com.bilibili.im.websocket.protocol;

import com.bilibili.im.app.ImApplicationService;
import com.bilibili.im.message.model.command.SendMessageCommand;
import com.bilibili.im.message.model.dto.MessageContentDTO;
import com.bilibili.im.message.model.vo.SendMessageVO;
import com.bilibili.im.websocket.metrics.ImWebSocketMetricsRecorder;
import com.bilibili.im.websocket.model.dto.ImWebSocketInboundMessageDTO;
import com.bilibili.im.websocket.model.enums.ImWebSocketMessageType;
import com.bilibili.im.websocket.service.ImRealtimePushIdempotencyService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImProtocolDispatcherTest {

    @Test
    void shouldPassConversationTypeToSendMessageCommand() {
        ImApplicationService imApplicationService = mock(ImApplicationService.class);
        ImRealtimePushIdempotencyService idempotencyService = mock(ImRealtimePushIdempotencyService.class);
        ImWebSocketMetricsRecorder metricsRecorder = mock(ImWebSocketMetricsRecorder.class);
        ImProtocolResponseFactory responseFactory = new ImProtocolResponseFactory();

        ImProtocolDispatcher dispatcher = new ImProtocolDispatcher(
                imApplicationService,
                idempotencyService,
                metricsRecorder,
                responseFactory
        );

        ImWebSocketInboundMessageDTO inbound = new ImWebSocketInboundMessageDTO();
        inbound.setType(ImWebSocketMessageType.SEND_MESSAGE.getCode());
        inbound.setConversationType(2);
        inbound.setReceiverId(123L);
        inbound.setClientMessageId(456L);
        inbound.setMessageType(1);
        MessageContentDTO content = new MessageContentDTO();
        content.setText("hello");
        inbound.setContent(content);

        when(idempotencyService.tryAcquire(1001L, 456L)).thenReturn(true);
        when(imApplicationService.acceptMessage(eq(1001L), eq("127.0.0.1"), any(SendMessageCommand.class)))
                .thenReturn(new SendMessageVO());

        dispatcher.dispatch(1001L, "127.0.0.1", inbound);

        ArgumentCaptor<SendMessageCommand> commandCaptor = ArgumentCaptor.forClass(SendMessageCommand.class);
        verify(imApplicationService).acceptMessage(eq(1001L), eq("127.0.0.1"), commandCaptor.capture());

        SendMessageCommand command = commandCaptor.getValue();
        assertNotNull(command);
        assertEquals(2, command.getConversationType());
        assertEquals(123L, command.getReceiverId());
        assertEquals(456L, command.getClientMessageId());
    }
}
