package com.bilibili.im.app;

import com.bilibili.im.mq.event.ImMessageDispatchEvent;

public interface GroupMessagePushApplicationService {

    void pushGroupMessage(ImMessageDispatchEvent event);
}
