package com.bilibili.im.group.service;

import com.bilibili.im.group.model.entity.ChatGroupDO;
import com.bilibili.im.group.model.entity.ChatGroupMemberDO;

import java.util.List;

public interface ChatGroupService {

    ChatGroupDO createGroup(Long ownerUserId, String groupName);

    ChatGroupDO getGroup(Long groupId);

    void inviteMember(Long groupId, Long targetUserId);

    void leaveGroup(Long groupId, Long currentUserId);

    void dismissGroup(Long groupId, Long ownerUserId);

    void kickGroupMember(Long groupId, Long operatorUserId, Long targetUserId);

    String updateGroupName(Long groupId, Long operatorUserId, String groupName);

    String updateGroupAvatar(Long groupId, Long operatorUserId, String groupAvatar);

    ChatGroupMemberDO getMembership(Long groupId, Long userId);

    List<ChatGroupMemberDO> listActiveMembers(Long groupId);
}
