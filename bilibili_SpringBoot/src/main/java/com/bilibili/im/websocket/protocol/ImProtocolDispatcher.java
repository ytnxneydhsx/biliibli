package com.bilibili.im.websocket.protocol;

import com.bilibili.im.app.ImApplicationService;
import com.bilibili.im.message.model.command.SendMessageCommand;
import com.bilibili.im.message.model.vo.SendMessageVO;
import com.bilibili.im.websocket.metrics.ImWebSocketMetricsRecorder;
import com.bilibili.im.websocket.model.dto.ImWebSocketInboundMessageDTO;
import com.bilibili.im.websocket.model.dto.ImWebSocketOutboundMessageDTO;
import com.bilibili.im.websocket.model.enums.ImWebSocketMessageType;
import com.bilibili.im.websocket.service.ImRealtimePushIdempotencyService;
import org.springframework.stereotype.Component;

@Component
public class ImProtocolDispatcher {

    private final ImApplicationService imApplicationService;
    private final ImRealtimePushIdempotencyService realtimePushIdempotencyService;
    private final ImWebSocketMetricsRecorder metricsRecorder;
    private final ImProtocolResponseFactory responseFactory;

    public ImProtocolDispatcher(ImApplicationService imApplicationService,
                                ImRealtimePushIdempotencyService realtimePushIdempotencyService,
                                ImWebSocketMetricsRecorder metricsRecorder,
                                ImProtocolResponseFactory responseFactory) {
        this.imApplicationService = imApplicationService;
        this.realtimePushIdempotencyService = realtimePushIdempotencyService;
        this.metricsRecorder = metricsRecorder;
        this.responseFactory = responseFactory;
    }

    public ImWebSocketOutboundMessageDTO dispatch(Long userId,
                                                  String clientIp,
                                                  ImWebSocketInboundMessageDTO inboundMessage) {
        if (ImWebSocketMessageType.matches(inboundMessage.getType(), ImWebSocketMessageType.HEARTBEAT)) {
            metricsRecorder.recordHeartbeatReceived();
            metricsRecorder.recordHeartbeatAckSent();
            return responseFactory.heartbeatAck();
        }

        if (ImWebSocketMessageType.matches(inboundMessage.getType(), ImWebSocketMessageType.SEND_MESSAGE)) {
            try {
                boolean acquired = realtimePushIdempotencyService.tryAcquire(
                        userId,
                        inboundMessage.getClientMessageId()
                );
                if (!acquired) {
                    return responseFactory.error("websocket message is duplicated");
                }

                SendMessageVO sendMessageVO = imApplicationService.acceptMessage(
                        userId,
                        clientIp,
                        toSendMessageCommand(inboundMessage)
                );
                return responseFactory.sendMessageAccepted(sendMessageVO);
            } catch (Exception ex) {
                return responseFactory.error(ex.getMessage());
            }
        }

        metricsRecorder.recordInboundTypeUnsupported();
        return responseFactory.error("websocket message type is unsupported");
    }

    private SendMessageCommand toSendMessageCommand(ImWebSocketInboundMessageDTO inboundMessage) {
        SendMessageCommand command = new SendMessageCommand();
        command.setConversationType(inboundMessage.getConversationType());
        command.setReceiverId(inboundMessage.getReceiverId());
        command.setClientMessageId(inboundMessage.getClientMessageId());
        command.setMessageType(inboundMessage.getMessageType());
        command.setContent(inboundMessage.getContent());
        return command;
    }

}
