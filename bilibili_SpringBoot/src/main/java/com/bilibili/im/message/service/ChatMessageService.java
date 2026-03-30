package com.bilibili.im.message.service;

import com.bilibili.im.message.model.command.PersistMessageCommand;
import com.bilibili.im.message.model.entity.ChatMessageDO;
import com.bilibili.im.message.model.vo.MessageHistoryVO;

public interface ChatMessageService {

    ChatMessageDO persistMessage(PersistMessageCommand command);

    MessageHistoryVO querySingleMessageHistory(Long ownerUserId, Long peerUserId, Long beforeMessageId, Integer pageSize);
}
