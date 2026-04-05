package com.bilibili.im.app;

import com.bilibili.im.group.model.dto.CreateChatGroupDTO;
import com.bilibili.im.group.model.dto.InviteGroupMemberDTO;
import com.bilibili.im.group.model.dto.UpdateChatGroupNameDTO;
import com.bilibili.im.group.model.vo.ChatGroupMemberListVO;
import com.bilibili.im.group.model.vo.ChatGroupVO;

public interface GroupApplicationService {

    ChatGroupVO createGroup(Long currentUserId, CreateChatGroupDTO dto);

    ChatGroupVO getGroup(Long groupId);

    void inviteGroupMember(Long currentUserId, Long groupId, InviteGroupMemberDTO dto);

    String updateGroupName(Long currentUserId, Long groupId, UpdateChatGroupNameDTO dto);

    void leaveGroup(Long currentUserId, Long groupId);

    void kickGroupMember(Long currentUserId, Long groupId, Long targetUserId);

    ChatGroupMemberListVO listGroupMembers(Long currentUserId, Long groupId);
}
