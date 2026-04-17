package com.bilibili.im.mq.consumer.single;

import com.bilibili.common.logging.LogContext;
import com.bilibili.im.message.cache.RecentMessageCacheService;
import com.bilibili.im.message.model.vo.MessageVO;
import com.bilibili.im.mq.ImMqLogContext;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import com.bilibili.im.mq.metrics.ImMqConsumerMetrics;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static com.bilibili.im.mq.metrics.ImMqConsumerMetrics.Consumer.SINGLE_RECENT_MESSAGE_CACHE_PROJECTION;

@Component
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class RecentMessageCacheProjectionConsumer {

    private final RecentMessageCacheService recentMessageCacheService;
    private final ImMqConsumerMetrics imMqConsumerMetrics;

    public RecentMessageCacheProjectionConsumer(RecentMessageCacheService recentMessageCacheService,
                                                ImMqConsumerMetrics imMqConsumerMetrics) {
        this.recentMessageCacheService = recentMessageCacheService;
        this.imMqConsumerMetrics = imMqConsumerMetrics;
    }

    @RabbitListener(
            queues = "#{@imMqProperties.recentMessageCacheProjectionQueue}",
            containerFactory = "imRedisProjectionListenerContainerFactory"
    )
    public void consume(ImMessageDispatchEvent event,
                        @Header(name = LogContext.TRACE_ID_HEADER, required = false) String traceId,
                        @Header(name = LogContext.UID_HEADER, required = false) String uid) {
        try (LogContext.Scope ignored = ImMqLogContext.open(event, traceId, uid)) {
            imMqConsumerMetrics.record(SINGLE_RECENT_MESSAGE_CACHE_PROJECTION, event, () -> {
                if (event == null) {
                    throw new IllegalArgumentException("event is invalid");
                }
                recentMessageCacheService.appendMessageIfInitialized(
                        event.getConversationId(),
                        toMessageVO(event)
                );
            });
        }
    }

    private static MessageVO toMessageVO(ImMessageDispatchEvent event) {
        MessageVO message = new MessageVO();
        message.setId(null);
        message.setServerMessageId(event.getServerMessageId());
        message.setConversationId(event.getConversationId());
        message.setSenderId(event.getSenderId());
        message.setReceiverId(event.getReceiverId());
        message.setClientMessageId(event.getClientMessageId());
        message.setSenderLocation(event.getSenderLocation());
        message.setMessageType(event.getMessageType());
        message.setContent(event.getContent());
        message.setSendTime(event.getSendTime());
        message.setStatus(null);
        return message;
    }
}
