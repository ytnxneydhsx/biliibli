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
        long dispatchStartNanos = System.nanoTime();
        String type = inboundMessage == null ? "unknown" : inboundMessage.getType();
        String outcome = "success";
        try {
            if (ImWebSocketMessageType.matches(type, ImWebSocketMessageType.HEARTBEAT)) {
                metricsRecorder.recordHeartbeatReceived();
                metricsRecorder.recordHeartbeatAckSent();
                return responseFactory.heartbeatAck();
            }

            if (ImWebSocketMessageType.matches(type, ImWebSocketMessageType.SEND_MESSAGE)) {
                Long clientMessageId = inboundMessage.getClientMessageId();
                try {
                    long idempotencyStartNanos = System.nanoTime();
                    boolean acquired;
                    try {
                        acquired = realtimePushIdempotencyService.tryAcquire(
                                userId,
                                clientMessageId
                        );
                    } finally {
                        metricsRecorder.recordProtocolIdempotency("success", System.nanoTime() - idempotencyStartNanos);
                    }
                    if (!acquired) {
                        outcome = "duplicate";
                        return responseFactory.error("websocket message is duplicated", clientMessageId);
                    }

                    SendMessageVO sendMessageVO;
                    long acceptStartNanos = System.nanoTime();
                    try {
                        sendMessageVO = imApplicationService.acceptMessage(
                                userId,
                                clientIp,
                                toSendMessageCommand(inboundMessage)
                        );
                        metricsRecorder.recordProtocolAcceptCall("success", System.nanoTime() - acceptStartNanos);
                    } catch (Exception ex) {
                        metricsRecorder.recordProtocolAcceptCall("failure", System.nanoTime() - acceptStartNanos);
                        throw ex;
                    }
                    return responseFactory.sendMessageAccepted(sendMessageVO);
                } catch (Exception ex) {
                    outcome = "error";
                    return responseFactory.error(ex.getMessage(), clientMessageId);
                }
            }

            metricsRecorder.recordInboundTypeUnsupported();
            outcome = "unsupported";
            return responseFactory.error("websocket message type is unsupported");
        } finally {
            metricsRecorder.recordProtocolDispatch(type, outcome, System.nanoTime() - dispatchStartNanos);
        }
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
