package com.bilibili.im.app;

import com.bilibili.im.group.model.dto.CreateChatGroupDTO;
import com.bilibili.im.group.model.vo.ChatGroupMemberListVO;
import com.bilibili.im.group.model.vo.ChatGroupVO;

public interface GroupApplicationService {

    ChatGroupVO createGroup(Long currentUserId, CreateChatGroupDTO dto);

    ChatGroupVO getGroup(Long groupId);

    ChatGroupMemberListVO listGroupMembers(Long currentUserId, Long groupId);
}
