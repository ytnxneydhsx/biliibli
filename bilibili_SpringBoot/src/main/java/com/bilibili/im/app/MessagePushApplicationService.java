package com.bilibili.im.app;

import com.bilibili.im.mq.event.ImMessageDispatchEvent;

public interface MessagePushApplicationService {

    void pushMessageToReceiver(ImMessageDispatchEvent event);

    void pushMessageToSender(ImMessageDispatchEvent event);
}
