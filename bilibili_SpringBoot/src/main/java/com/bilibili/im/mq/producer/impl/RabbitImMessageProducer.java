package com.bilibili.im.mq.producer.impl;

import com.bilibili.config.properties.ImMqProperties;
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
        rabbitTemplate.convertAndSend(
                imMqProperties.getExchange(),
                imMqProperties.getRoutingKey(),
                event
        );
    }
}
