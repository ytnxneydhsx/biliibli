package com.bilibili.im.message.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.im.message.model.vo.MessageHistoryVO;
import com.bilibili.im.message.service.GroupMessageQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/im/groups")
@PreAuthorize("isAuthenticated()")
@Tag(name = "IM Group Message", description = "Current user IM group message APIs")
public class ImGroupMessageController {

    private final GroupMessageQueryService groupMessageQueryService;

    public ImGroupMessageController(GroupMessageQueryService groupMessageQueryService) {
        this.groupMessageQueryService = groupMessageQueryService;
    }

    @GetMapping("/{groupId}/messages/history")
    @Operation(summary = "Query current group message history")
    public Result<MessageHistoryVO> queryGroupMessageHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("groupId")
            @NotNull(message = "groupId cannot be null")
            @Positive(message = "groupId must be positive") Long groupId,
            Long beforeServerMessageId) {
        return Result.success(groupMessageQueryService.queryGroupMessageHistory(
                currentUser.getUid(),
                groupId,
                beforeServerMessageId
        ));
    }
}
