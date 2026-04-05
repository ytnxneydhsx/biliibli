package com.bilibili.im.group.service;

import com.bilibili.im.group.model.entity.ChatGroupDO;
import com.bilibili.im.group.model.entity.ChatGroupMemberDO;

import java.util.List;

public interface ChatGroupService {

    ChatGroupDO createGroup(Long ownerUserId, String groupName);

    ChatGroupDO getGroup(Long groupId);

    ChatGroupMemberDO getMembership(Long groupId, Long userId);

    List<ChatGroupMemberDO> listActiveMembers(Long groupId);
}
