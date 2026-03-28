package com.bilibili.config.mq;

import com.bilibili.config.properties.ImMqProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        return new Queue(properties.getMessagePersistQueue(), true);
    }

    @Bean
    public Queue conversationProjectionQueue(ImMqProperties properties) {
        return new Queue(properties.getConversationProjectionQueue(), true);
    }

    @Bean
    public Binding realtimePushBinding(Queue realtimePushQueue,
                                       TopicExchange imEventExchange,
                                       ImMqProperties properties) {
        return BindingBuilder.bind(realtimePushQueue)
                .to(imEventExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    public Binding messagePersistBinding(Queue messagePersistQueue,
                                         TopicExchange imEventExchange,
                                         ImMqProperties properties) {
        return BindingBuilder.bind(messagePersistQueue)
                .to(imEventExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    public Binding conversationProjectionBinding(Queue conversationProjectionQueue,
                                                 TopicExchange imEventExchange,
                                                 ImMqProperties properties) {
        return BindingBuilder.bind(conversationProjectionQueue)
                .to(imEventExchange)
                .with(properties.getRoutingKey());
    }
}
