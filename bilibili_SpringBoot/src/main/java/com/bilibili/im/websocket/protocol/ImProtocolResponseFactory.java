package com.bilibili.im.websocket.protocol;

import com.bilibili.im.websocket.model.dto.ImWebSocketOutboundMessageDTO;
import com.bilibili.im.websocket.model.enums.ImWebSocketMessageType;
import org.springframework.stereotype.Component;

@Component
public class ImProtocolResponseFactory {

    public ImWebSocketOutboundMessageDTO heartbeatAck() {
        return simpleMessage(ImWebSocketMessageType.HEARTBEAT_ACK, "OK", 0, null);
    }

    public ImWebSocketOutboundMessageDTO sendMessageAccepted(Object data) {
        return simpleMessage(ImWebSocketMessageType.SEND_MESSAGE_ACCEPTED, "OK", 0, data);
    }

    public ImWebSocketOutboundMessageDTO error(String message) {
        return error(message, null);
    }

    public ImWebSocketOutboundMessageDTO error(String message, Long clientMessageId) {
        return simpleMessage(
                ImWebSocketMessageType.ERROR,
                message == null || message.isBlank() ? "websocket message handling failed" : message,
                1,
                clientMessageId != null ? java.util.Map.of("clientMessageId", clientMessageId) : null
        );
    }

    private ImWebSocketOutboundMessageDTO simpleMessage(ImWebSocketMessageType type,
                                                        String message,
                                                        Integer code,
                                                        Object data) {
        ImWebSocketOutboundMessageDTO outboundMessage = new ImWebSocketOutboundMessageDTO();
        outboundMessage.setType(type.getCode());
        outboundMessage.setCode(code);
        outboundMessage.setMessage(message);
        outboundMessage.setData(data);
        return outboundMessage;
    }
}
