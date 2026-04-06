package com.bilibili.im.group.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.im.app.GroupApplicationService;
import com.bilibili.im.group.model.dto.CreateChatGroupDTO;
import com.bilibili.im.group.model.dto.InviteGroupMemberDTO;
import com.bilibili.im.group.model.dto.UpdateChatGroupMemberRoleDTO;
import com.bilibili.im.group.model.dto.UpdateChatGroupMuteStatusDTO;
import com.bilibili.im.group.model.dto.UpdateChatGroupNameDTO;
import com.bilibili.im.group.model.vo.ChatGroupMemberListVO;
import com.bilibili.im.group.model.vo.ChatGroupVO;
import com.bilibili.im.group.service.ChatGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/im")
@PreAuthorize("isAuthenticated()")
@Tag(name = "IM Group", description = "Current user IM group APIs")
public class ImGroupController {

    private final GroupApplicationService groupApplicationService;
    private final ChatGroupService chatGroupService;

    public ImGroupController(GroupApplicationService groupApplicationService,
                             ChatGroupService chatGroupService) {
        this.groupApplicationService = groupApplicationService;
        this.chatGroupService = chatGroupService;
    }

    @PostMapping("/create-group")
    @Operation(summary = "Create a group conversation")
    public Result<ChatGroupVO> createGroup(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateChatGroupDTO dto) {
        return Result.success(groupApplicationService.createGroup(currentUser.getUid(), dto));
    }

    @GetMapping("/groups/{groupId}")
    @Operation(summary = "Get group profile")
    public Result<ChatGroupVO> getGroup(
            @PathVariable("groupId")
            @NotNull(message = "groupId cannot be null")
            @Positive(message = "groupId must be positive") Long groupId) {
        return Result.success(groupApplicationService.getGroup(groupId));
    }

    @PostMapping("/groups/{groupId}/members")
    @Operation(summary = "Invite a member into group")
    public Result<Void> inviteGroupMember(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("groupId")
            @NotNull(message = "groupId cannot be null")
            @Positive(message = "groupId must be positive") Long groupId,
            @Valid @RequestBody InviteGroupMemberDTO dto) {
        groupApplicationService.inviteGroupMember(currentUser.getUid(), groupId, dto);
        return Result.success(null);
    }

    @PutMapping("/groups/{groupId}/name")
    @Operation(summary = "Update current group name")
    public Result<String> updateGroupName(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("groupId")
            @NotNull(message = "groupId cannot be null")
            @Positive(message = "groupId must be positive") Long groupId,
            @Valid @RequestBody UpdateChatGroupNameDTO dto) {
        return Result.success(groupApplicationService.updateGroupName(currentUser.getUid(), groupId, dto));
    }

    @PutMapping("/groups/{groupId}/mute")
    @Operation(summary = "Update current group all mute status")
    public Result<Void> updateGroupMuteStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("groupId")
            @NotNull(message = "groupId cannot be null")
            @Positive(message = "groupId must be positive") Long groupId,
            @Valid @RequestBody UpdateChatGroupMuteStatusDTO dto) {
        chatGroupService.updateGroupMuteStatus(groupId, currentUser.getUid(), dto.getIsMuted());
        return Result.success(null);
    }

    @PostMapping("/groups/{groupId}/leave")
    @Operation(summary = "Leave current group")
    public Result<Void> leaveGroup(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("groupId")
            @NotNull(message = "groupId cannot be null")
            @Positive(message = "groupId must be positive") Long groupId) {
        groupApplicationService.leaveGroup(currentUser.getUid(), groupId);
        return Result.success(null);
    }

    @DeleteMapping("/groups/{groupId}/members/{targetUserId}")
    @Operation(summary = "Kick a member from current group")
    public Result<Void> kickGroupMember(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("groupId")
            @NotNull(message = "groupId cannot be null")
            @Positive(message = "groupId must be positive") Long groupId,
            @PathVariable("targetUserId")
            @NotNull(message = "targetUserId cannot be null")
            @Positive(message = "targetUserId must be positive") Long targetUserId) {
        groupApplicationService.kickGroupMember(currentUser.getUid(), groupId, targetUserId);
        return Result.success(null);
    }

    @PutMapping("/groups/{groupId}/members/{targetUserId}/role")
    @Operation(summary = "Update current group member role")
    public Result<Void> updateGroupMemberRole(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("groupId")
            @NotNull(message = "groupId cannot be null")
            @Positive(message = "groupId must be positive") Long groupId,
            @PathVariable("targetUserId")
            @NotNull(message = "targetUserId cannot be null")
            @Positive(message = "targetUserId must be positive") Long targetUserId,
            @Valid @RequestBody UpdateChatGroupMemberRoleDTO dto) {
        chatGroupService.updateGroupMemberRole(groupId, currentUser.getUid(), targetUserId, dto.getRole());
        return Result.success(null);
    }

    @PutMapping("/groups/{groupId}/members/{targetUserId}/mute")
    @Operation(summary = "Update current group member mute status")
    public Result<Void> updateGroupMemberMuteStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("groupId")
            @NotNull(message = "groupId cannot be null")
            @Positive(message = "groupId must be positive") Long groupId,
            @PathVariable("targetUserId")
            @NotNull(message = "targetUserId cannot be null")
            @Positive(message = "targetUserId must be positive") Long targetUserId,
            @Valid @RequestBody UpdateChatGroupMuteStatusDTO dto) {
        chatGroupService.updateGroupMemberMuteStatus(groupId, currentUser.getUid(), targetUserId, dto.getIsMuted());
        return Result.success(null);
    }

    @GetMapping("/groups/{groupId}/members")
    @Operation(summary = "List active group members")
    public Result<ChatGroupMemberListVO> listGroupMembers(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("groupId")
            @NotNull(message = "groupId cannot be null")
            @Positive(message = "groupId must be positive") Long groupId) {
        return Result.success(groupApplicationService.listGroupMembers(currentUser.getUid(), groupId));
    }
}
