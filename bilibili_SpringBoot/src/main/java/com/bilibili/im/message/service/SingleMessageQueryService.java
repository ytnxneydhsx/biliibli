package com.bilibili.im.message.service;

import com.bilibili.im.message.model.vo.MessageHistoryVO;

public interface SingleMessageQueryService {

    MessageHistoryVO querySingleMessageHistory(Long ownerUserId, Long peerUserId, Long beforeServerMessageId);
}
