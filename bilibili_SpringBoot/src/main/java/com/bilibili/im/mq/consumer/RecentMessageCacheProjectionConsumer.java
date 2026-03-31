package com.bilibili.im.mq.consumer;

import com.bilibili.im.message.cache.RecentMessageCacheService;
import com.bilibili.im.message.model.vo.MessageVO;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class RecentMessageCacheProjectionConsumer {

    private final RecentMessageCacheService recentMessageCacheService;

    public RecentMessageCacheProjectionConsumer(RecentMessageCacheService recentMessageCacheService) {
        this.recentMessageCacheService = recentMessageCacheService;
    }

    @RabbitListener(queues = "#{@imMqProperties.recentMessageCacheProjectionQueue}")
    public void consume(ImMessageDispatchEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is invalid");
        }
        recentMessageCacheService.appendMessageIfInitialized(
                event.getConversationId(),
                toMessageVO(event)
        );
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
