package com.bilibili.im.conversation.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.im.app.ConversationWindowApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/im/conversations")
@PreAuthorize("isAuthenticated()")
@Tag(name = "IM Conversation", description = "Current user IM conversation APIs")
public class ImConversationController {

    private final ConversationWindowApplicationService conversationWindowApplicationService;

    public ImConversationController(ConversationWindowApplicationService conversationWindowApplicationService) {
        this.conversationWindowApplicationService = conversationWindowApplicationService;
    }

    @PostMapping("/read")
    @PreAuthorize("@accessAuthz.canSendImMessage(authentication)")
    @Operation(summary = "Clear unread count for a single conversation")
    public Result<Void> readSingleConversation(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam("targetId") @NotNull(message = "targetId cannot be null")
            @Positive(message = "targetId must be positive") Long targetId) {
        conversationWindowApplicationService.clearSingleConversationUnread(currentUser.getUid(), targetId);
        return Result.success(null);
    }
}
