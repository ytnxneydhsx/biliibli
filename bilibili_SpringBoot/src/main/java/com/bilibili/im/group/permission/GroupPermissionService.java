package com.bilibili.im.group.permission;

import com.bilibili.im.group.model.entity.ChatGroupDO;
import com.bilibili.im.group.model.entity.ChatGroupMemberDO;

public interface GroupPermissionService {

    ChatGroupDO requireActiveGroup(Long groupId);

    ChatGroupMemberDO requireActiveMembership(Long groupId, Long userId);

    void requireCanManageProfile(ChatGroupMemberDO membership);

    void requireCanDismissGroup(ChatGroupDO group, ChatGroupMemberDO membership, Long operatorUserId);

    void requireCanKickMember(ChatGroupMemberDO operatorMembership,
                              ChatGroupMemberDO targetMembership,
                              Long operatorUserId,
                              Long targetUserId);

    void requireCanChangeMemberRole(ChatGroupMemberDO operatorMembership,
                                    ChatGroupMemberDO targetMembership,
                                    Integer targetRole);
}
