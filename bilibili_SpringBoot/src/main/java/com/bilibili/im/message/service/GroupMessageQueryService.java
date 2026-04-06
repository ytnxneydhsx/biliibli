package com.bilibili.im.message.service;

import com.bilibili.im.message.model.vo.MessageHistoryVO;

public interface GroupMessageQueryService {

    MessageHistoryVO queryGroupMessageHistory(Long ownerUserId, Long groupId, Long beforeServerMessageId);
}
