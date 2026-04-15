package com.bilibili.config.mq;

import com.bilibili.config.properties.ImMqProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class ImRabbitMqConfig {

    @Bean
    public Jackson2JsonMessageConverter imMqMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange imEventExchange(ImMqProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean
    public Queue realtimePushQueue(ImMqProperties properties) {
        return new Queue(properties.getRealtimePushQueue(), true);
    }

    @Bean
    public Queue messagePersistQueue(ImMqProperties properties) {
        return QueueBuilder.durable(properties.getMessagePersistQueue())
                .deadLetterExchange("im.message.dlx")
                .deadLetterRoutingKey(properties.getMessagePersistQueue() + ".dlq")
                .build();
    }

    @Bean
    public Queue conversationPersistQueue(ImMqProperties properties) {
        return QueueBuilder.durable(properties.getConversationPersistQueue())
                .deadLetterExchange("im.message.dlx")
                .deadLetterRoutingKey(properties.getConversationPersistQueue() + ".dlq")
                .build();
    }

    @Bean
    public Queue conversationRedisProjectionQueue(ImMqProperties properties) {
        return new Queue(properties.getConversationRedisProjectionQueue(), true);
    }

    @Bean
    public Queue recentMessageCacheProjectionQueue(ImMqProperties properties) {
        return new Queue(properties.getRecentMessageCacheProjectionQueue(), true);
    }

    @Bean
    public Queue groupRealtimePushQueue(ImMqProperties properties) {
        return new Queue(properties.getGroupRealtimePushQueue(), true);
    }

    @Bean
    public Queue groupMessagePersistQueue(ImMqProperties properties) {
        return QueueBuilder.durable(properties.getGroupMessagePersistQueue())
                .deadLetterExchange("im.message.dlx")
                .deadLetterRoutingKey(properties.getGroupMessagePersistQueue() + ".dlq")
                .build();
    }

    @Bean
    public Queue groupConversationPersistQueue(ImMqProperties properties) {
        return new Queue(properties.getGroupConversationPersistQueue(), true);
    }

    @Bean
    public Queue groupConversationRedisProjectionQueue(ImMqProperties properties) {
        return new Queue(properties.getGroupConversationRedisProjectionQueue(), true);
    }

    @Bean
    public Queue groupRecentMessageCacheProjectionQueue(ImMqProperties properties) {
        return new Queue(properties.getGroupRecentMessageCacheProjectionQueue(), true);
    }

    @Bean
    public Binding realtimePushBinding(@Qualifier("realtimePushQueue") Queue realtimePushQueue,
                                       TopicExchange imEventExchange,
                                       ImMqProperties properties) {
        return BindingBuilder.bind(realtimePushQueue)
                .to(imEventExchange)
                .with(properties.getSingleRoutingKey());
    }

    @Bean
    public Binding messagePersistBinding(@Qualifier("messagePersistQueue") Queue messagePersistQueue,
                                         TopicExchange imEventExchange,
                                         ImMqProperties properties) {
        return BindingBuilder.bind(messagePersistQueue)
                .to(imEventExchange)
                .with(properties.getSingleRoutingKey());
    }

    @Bean
    public Binding conversationPersistBinding(@Qualifier("conversationPersistQueue") Queue conversationPersistQueue,
                                              TopicExchange imEventExchange,
                                              ImMqProperties properties) {
        return BindingBuilder.bind(conversationPersistQueue)
                .to(imEventExchange)
                .with(properties.getSingleRoutingKey());
    }

    @Bean
    public Binding conversationRedisProjectionBinding(@Qualifier("conversationRedisProjectionQueue") Queue conversationRedisProjectionQueue,
                                                      TopicExchange imEventExchange,
                                                      ImMqProperties properties) {
        return BindingBuilder.bind(conversationRedisProjectionQueue)
                .to(imEventExchange)
                .with(properties.getSingleRoutingKey());
    }

    @Bean
    public Binding recentMessageCacheProjectionBinding(@Qualifier("recentMessageCacheProjectionQueue") Queue recentMessageCacheProjectionQueue,
                                                       TopicExchange imEventExchange,
                                                       ImMqProperties properties) {
        return BindingBuilder.bind(recentMessageCacheProjectionQueue)
                .to(imEventExchange)
                .with(properties.getSingleRoutingKey());
    }

    @Bean
    public Binding groupRealtimePushBinding(@Qualifier("groupRealtimePushQueue") Queue groupRealtimePushQueue,
                                            TopicExchange imEventExchange,
                                            ImMqProperties properties) {
        return BindingBuilder.bind(groupRealtimePushQueue)
                .to(imEventExchange)
                .with(properties.getGroupRoutingKey());
    }

    @Bean
    public Binding groupMessagePersistBinding(@Qualifier("groupMessagePersistQueue") Queue groupMessagePersistQueue,
                                              TopicExchange imEventExchange,
                                              ImMqProperties properties) {
        return BindingBuilder.bind(groupMessagePersistQueue)
                .to(imEventExchange)
                .with(properties.getGroupRoutingKey());
    }

    @Bean
    public Binding groupConversationPersistBinding(@Qualifier("groupConversationPersistQueue") Queue groupConversationPersistQueue,
                                                   TopicExchange imEventExchange,
                                                   ImMqProperties properties) {
        return BindingBuilder.bind(groupConversationPersistQueue)
                .to(imEventExchange)
                .with(properties.getGroupRoutingKey());
    }

    @Bean
    public Binding groupConversationRedisProjectionBinding(@Qualifier("groupConversationRedisProjectionQueue") Queue groupConversationRedisProjectionQueue,
                                                           TopicExchange imEventExchange,
                                                           ImMqProperties properties) {
        return BindingBuilder.bind(groupConversationRedisProjectionQueue)
                .to(imEventExchange)
                .with(properties.getGroupRoutingKey());
    }

    @Bean
    public Binding groupRecentMessageCacheProjectionBinding(@Qualifier("groupRecentMessageCacheProjectionQueue") Queue groupRecentMessageCacheProjectionQueue,
                                                            TopicExchange imEventExchange,
                                                            ImMqProperties properties) {
        return BindingBuilder.bind(groupRecentMessageCacheProjectionQueue)
                .to(imEventExchange)
                .with(properties.getGroupRoutingKey());
    }

    // ==================== 死信交换机 + 死信队列 ====================

    @Bean
    public DirectExchange imDeadLetterExchange() {
        return new DirectExchange("im.message.dlx", true, false);
    }

    @Bean
    public Queue messagePersistDlq(ImMqProperties properties) {
        return QueueBuilder.durable(properties.getMessagePersistQueue() + ".dlq").build();
    }

    @Bean
    public Queue conversationPersistDlq(ImMqProperties properties) {
        return QueueBuilder.durable(properties.getConversationPersistQueue() + ".dlq").build();
    }

    @Bean
    public Queue groupMessagePersistDlq(ImMqProperties properties) {
        return QueueBuilder.durable(properties.getGroupMessagePersistQueue() + ".dlq").build();
    }

    @Bean
    public Binding messagePersistDlqBinding(@Qualifier("messagePersistDlq") Queue dlq,
                                             DirectExchange imDeadLetterExchange,
                                             ImMqProperties properties) {
        return BindingBuilder.bind(dlq)
                .to(imDeadLetterExchange)
                .with(properties.getMessagePersistQueue() + ".dlq");
    }

    @Bean
    public Binding conversationPersistDlqBinding(@Qualifier("conversationPersistDlq") Queue dlq,
                                                  DirectExchange imDeadLetterExchange,
                                                  ImMqProperties properties) {
        return BindingBuilder.bind(dlq)
                .to(imDeadLetterExchange)
                .with(properties.getConversationPersistQueue() + ".dlq");
    }

    @Bean
    public Binding groupMessagePersistDlqBinding(@Qualifier("groupMessagePersistDlq") Queue dlq,
                                                  DirectExchange imDeadLetterExchange,
                                                  ImMqProperties properties) {
        return BindingBuilder.bind(dlq)
                .to(imDeadLetterExchange)
                .with(properties.getGroupMessagePersistQueue() + ".dlq");
    }
}
