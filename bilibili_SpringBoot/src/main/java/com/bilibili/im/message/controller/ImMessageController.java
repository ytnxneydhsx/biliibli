package com.bilibili.im.message.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.im.app.ImApplicationService;
import com.bilibili.im.message.model.dto.SendMessageDTO;
import com.bilibili.im.message.model.vo.SendMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/im/messages")
@PreAuthorize("isAuthenticated()")
@Tag(name = "IM Message", description = "Current user IM message APIs")
public class ImMessageController {

    private final ImApplicationService imApplicationService;

    public ImMessageController(ImApplicationService imApplicationService) {
        this.imApplicationService = imApplicationService;
    }

    @PostMapping
    @PreAuthorize("@accessAuthz.canSendImMessage(authentication)")
    @Operation(summary = "Send a private message")
    public Result<SendMessageVO> sendMessage(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody SendMessageDTO dto) {
        return Result.success(imApplicationService.acceptMessage(currentUser.getUid(), dto));
    }
}
