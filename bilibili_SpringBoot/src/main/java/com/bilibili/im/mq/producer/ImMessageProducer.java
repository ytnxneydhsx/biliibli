package com.bilibili.im.mq.producer;

import com.bilibili.im.mq.event.ImMessageDispatchEvent;

public interface ImMessageProducer {

    void publish(ImMessageDispatchEvent event);
}
