package com.bilibili.im.mq.producer.impl;

import com.bilibili.config.properties.ImMqProperties;
import com.bilibili.im.conversation.model.enums.ConversationType;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import com.bilibili.im.mq.producer.ImMessageProducer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class RabbitImMessageProducer implements ImMessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ImMqProperties imMqProperties;

    public RabbitImMessageProducer(RabbitTemplate rabbitTemplate, ImMqProperties imMqProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.imMqProperties = imMqProperties;
    }

    @Override
    public void publish(ImMessageDispatchEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is invalid");
        }
        String routingKey = resolveRoutingKey(event);
        rabbitTemplate.convertAndSend(
                imMqProperties.getExchange(),
                routingKey,
                event
        );
    }

    private String resolveRoutingKey(ImMessageDispatchEvent event) {
        Integer conversationType = event.getConversationType();
        if (Integer.valueOf(ConversationType.SINGLE.getCode()).equals(conversationType)) {
            return imMqProperties.getSingleRoutingKey();
        }
        if (Integer.valueOf(ConversationType.GROUP.getCode()).equals(conversationType)) {
            return imMqProperties.getGroupRoutingKey();
        }
        throw new IllegalArgumentException("conversationType is invalid");
    }
}
