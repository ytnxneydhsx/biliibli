package com.bilibili.im.mq.producer.impl;

import com.bilibili.config.properties.ImMqProperties;
import com.bilibili.im.conversation.model.enums.ConversationType;
import com.bilibili.im.metrics.ImSendMetrics;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import com.bilibili.im.mq.producer.ImMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class RabbitImMessageProducer implements ImMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(RabbitImMessageProducer.class);
    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;
    private final ImMqProperties imMqProperties;
    private final ImSendMetrics imSendMetrics;

    public RabbitImMessageProducer(RabbitTemplate rabbitTemplate,
                                   ImMqProperties imMqProperties,
                                   ImSendMetrics imSendMetrics) {
        this.rabbitTemplate = rabbitTemplate;
        this.imMqProperties = imMqProperties;
        this.imSendMetrics = imSendMetrics;
        this.rabbitTemplate.setMandatory(true);
        this.rabbitTemplate.setReturnsCallback(returned ->
                log.error("message returned: exchange={}, routingKey={}, replyText={}",
                        returned.getExchange(), returned.getRoutingKey(), returned.getReplyText())
        );
    }

    @Override
    public void publish(ImMessageDispatchEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is invalid");
        }
        String routingKey = resolveRoutingKey(event);
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        long publishStartNanos = System.nanoTime();

        try {
            long sendStartNanos = System.nanoTime();
            try {
                rabbitTemplate.convertAndSend(
                        imMqProperties.getExchange(),
                        routingKey,
                        event,
                        correlationData
                );
            } finally {
                imSendMetrics.recordMqPublishSend(System.nanoTime() - sendStartNanos);
            }

            try {
                long confirmStartNanos = System.nanoTime();
                CorrelationData.Confirm confirm;
                try {
                    confirm = correlationData.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } finally {
                    imSendMetrics.recordMqPublishConfirm(System.nanoTime() - confirmStartNanos);
                }
                if (confirm == null || !confirm.isAck()) {
                    String reason = confirm != null ? confirm.getReason() : "no confirm received";
                    imSendMetrics.recordMqPublishConfirmNack();
                    log.error("publisher confirm nack: correlationId={}, reason={}", correlationData.getId(), reason);
                    throw new RuntimeException("message publish was not confirmed by broker: " + reason);
                }
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                imSendMetrics.recordMqPublishConfirmTimeout();
                log.error("publisher confirm timeout: correlationId={}", correlationData.getId(), ex);
                throw new RuntimeException("message publish confirm timed out", ex);
            }
        } finally {
            imSendMetrics.recordMqPublishTotal(System.nanoTime() - publishStartNanos);
        }
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
