package com.bilibili.im.group.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.im.app.GroupApplicationService;
import com.bilibili.im.group.model.dto.CreateChatGroupDTO;
import com.bilibili.im.group.model.vo.ChatGroupMemberListVO;
import com.bilibili.im.group.model.vo.ChatGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/im")
@PreAuthorize("isAuthenticated()")
@Tag(name = "IM Group", description = "Current user IM group APIs")
public class ImGroupController {

    private final GroupApplicationService groupApplicationService;

    public ImGroupController(GroupApplicationService groupApplicationService) {
        this.groupApplicationService = groupApplicationService;
    }

    @PostMapping("/create-group")
    @PreAuthorize("@accessAuthz.canSendImMessage(authentication)")
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

    @GetMapping("/groups/{groupId}/members")
    @PreAuthorize("@accessAuthz.canSendImMessage(authentication)")
    @Operation(summary = "List active group members")
    public Result<ChatGroupMemberListVO> listGroupMembers(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("groupId")
            @NotNull(message = "groupId cannot be null")
            @Positive(message = "groupId must be positive") Long groupId) {
        return Result.success(groupApplicationService.listGroupMembers(currentUser.getUid(), groupId));
    }
}
