package com.bilibili.im.message.service;

import com.bilibili.im.message.model.command.PersistMessageCommand;
import com.bilibili.im.message.model.entity.ChatMessageDO;

public interface ChatMessageService {

    ChatMessageDO persistMessage(PersistMessageCommand command);
}
