package com.bilibili.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImMqProperties {

    @Value("${app.im.mq.enabled:false}")
    private boolean enabled;

    @Value("${app.im.mq.exchange:im.message.exchange}")
    private String exchange;

    @Value("${app.im.mq.routingKey:im.message.dispatch}")
    private String routingKey;

    @Value("${app.im.mq.realtimePushQueue:im.message.realtime.queue}")
    private String realtimePushQueue;

    @Value("${app.im.mq.messagePersistQueue:im.message.persist.queue}")
    private String messagePersistQueue;

    @Value("${app.im.mq.conversationPersistQueue:im.message.conversation.queue}")
    private String conversationPersistQueue;

    @Value("${app.im.mq.conversationRedisProjectionQueue:im.message.conversation.redis.queue}")
    private String conversationRedisProjectionQueue;

    public boolean isEnabled() {
        return enabled;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getRealtimePushQueue() {
        return realtimePushQueue;
    }

    public String getMessagePersistQueue() {
        return messagePersistQueue;
    }

    public String getConversationPersistQueue() {
        return conversationPersistQueue;
    }

    public String getConversationRedisProjectionQueue() {
        return conversationRedisProjectionQueue;
    }
}
