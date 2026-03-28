package com.bilibili.im.mq.producer.impl;

import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import com.bilibili.im.mq.producer.ImMessageProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopImMessageProducer implements ImMessageProducer {

    @Override
    public void publish(ImMessageDispatchEvent event) {
        throw new IllegalStateException("IM MQ is disabled");
    }
}
