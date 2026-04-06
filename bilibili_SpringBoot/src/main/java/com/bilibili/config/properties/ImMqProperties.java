package com.bilibili.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImMqProperties {

    @Value("${app.im.mq.enabled:false}")
    private boolean enabled;

    @Value("${app.im.mq.exchange:im.message.exchange}")
    private String exchange;

    @Value("${app.im.mq.singleRoutingKey:${app.im.mq.routingKey:im.message.single.dispatch}}")
    private String singleRoutingKey;

    @Value("${app.im.mq.groupRoutingKey:im.message.group.dispatch}")
    private String groupRoutingKey;

    @Value("${app.im.mq.realtimePushQueue:im.message.realtime.queue}")
    private String realtimePushQueue;

    @Value("${app.im.mq.messagePersistQueue:im.message.persist.queue}")
    private String messagePersistQueue;

    @Value("${app.im.mq.conversationPersistQueue:im.message.conversation.queue}")
    private String conversationPersistQueue;

    @Value("${app.im.mq.conversationRedisProjectionQueue:im.message.conversation.redis.queue}")
    private String conversationRedisProjectionQueue;

    @Value("${app.im.mq.recentMessageCacheProjectionQueue:im.message.recent.cache.queue}")
    private String recentMessageCacheProjectionQueue;

    @Value("${app.im.mq.groupRealtimePushQueue:im.message.group.realtime.queue}")
    private String groupRealtimePushQueue;

    @Value("${app.im.mq.groupMessagePersistQueue:im.message.group.persist.queue}")
    private String groupMessagePersistQueue;

    @Value("${app.im.mq.groupConversationPersistQueue:im.message.group.conversation.queue}")
    private String groupConversationPersistQueue;

    @Value("${app.im.mq.groupConversationRedisProjectionQueue:im.message.group.conversation.redis.queue}")
    private String groupConversationRedisProjectionQueue;

    @Value("${app.im.mq.groupRecentMessageCacheProjectionQueue:im.message.group.recent.cache.queue}")
    private String groupRecentMessageCacheProjectionQueue;

    public boolean isEnabled() {
        return enabled;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return singleRoutingKey;
    }

    public String getSingleRoutingKey() {
        return singleRoutingKey;
    }

    public String getGroupRoutingKey() {
        return groupRoutingKey;
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

    public String getRecentMessageCacheProjectionQueue() {
        return recentMessageCacheProjectionQueue;
    }

    public String getGroupRealtimePushQueue() {
        return groupRealtimePushQueue;
    }

    public String getGroupMessagePersistQueue() {
        return groupMessagePersistQueue;
    }

    public String getGroupConversationPersistQueue() {
        return groupConversationPersistQueue;
    }

    public String getGroupConversationRedisProjectionQueue() {
        return groupConversationRedisProjectionQueue;
    }

    public String getGroupRecentMessageCacheProjectionQueue() {
        return groupRecentMessageCacheProjectionQueue;
    }
}
