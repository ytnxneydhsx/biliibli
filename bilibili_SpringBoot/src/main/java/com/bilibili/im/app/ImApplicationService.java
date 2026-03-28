package com.bilibili.im.app;

import com.bilibili.im.message.model.dto.SendMessageDTO;
import com.bilibili.im.message.model.vo.SendMessageVO;

/**
 * IM application orchestration entry.
 * Concrete use cases are added here before dispatching to domain services.
 */
public interface ImApplicationService {

    SendMessageVO acceptMessage(Long senderId, SendMessageDTO dto);
}
