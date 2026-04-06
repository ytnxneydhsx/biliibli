package com.bilibili.im.message.service;

import com.bilibili.im.message.model.command.PersistMessageCommand;

public interface GroupMessagePersistService {

    void persistGroupMessage(Long groupId, PersistMessageCommand command);
}
