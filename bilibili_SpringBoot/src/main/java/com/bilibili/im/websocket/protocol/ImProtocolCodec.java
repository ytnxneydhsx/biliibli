package com.bilibili.im.websocket.protocol;

import com.bilibili.im.websocket.model.dto.ImWebSocketInboundMessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ImProtocolCodec {

    private final ObjectMapper objectMapper;

    public ImProtocolCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ImWebSocketInboundMessageDTO decodeInbound(String payload) throws Exception {
        return objectMapper.readValue(payload, ImWebSocketInboundMessageDTO.class);
    }

    public String encodeOutbound(Object payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }
}
