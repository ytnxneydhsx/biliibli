package com.bilibili.im.message.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.im.app.ImApplicationService;
import com.bilibili.im.message.model.command.SendMessageCommand;
import com.bilibili.im.message.model.dto.QueryMessageHistoryDTO;
import com.bilibili.im.message.model.dto.SendMessageDTO;
import com.bilibili.im.message.model.vo.MessageHistoryVO;
import com.bilibili.im.message.model.vo.SendMessageVO;
import com.bilibili.im.message.service.ChatMessageService;
import com.bilibili.security.resolver.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final ChatMessageService chatMessageService;
    private final ClientIpResolver clientIpResolver;

    public ImMessageController(ImApplicationService imApplicationService,
                               ChatMessageService chatMessageService,
                               ClientIpResolver clientIpResolver) {
        this.imApplicationService = imApplicationService;
        this.chatMessageService = chatMessageService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping
    @PreAuthorize("@accessAuthz.canSendImMessage(authentication)")
    @Operation(summary = "Send a private message")
    public Result<SendMessageVO> sendMessage(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            HttpServletRequest request,
            @Valid @RequestBody SendMessageDTO dto) {
        return Result.success(imApplicationService.acceptMessage(
                currentUser.getUid(),
                clientIpResolver.resolve(request),
                toCommand(dto)
        ));
    }

    @GetMapping("/history")
    @PreAuthorize("@accessAuthz.canSendImMessage(authentication)")
    @Operation(summary = "Query single conversation message history")
    public Result<MessageHistoryVO> queryMessageHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid QueryMessageHistoryDTO dto) {
        return Result.success(chatMessageService.querySingleMessageHistory(
                currentUser.getUid(),
                dto.getPeerUid(),
                dto.getBeforeServerMessageId()
        ));
    }

    private SendMessageCommand toCommand(SendMessageDTO dto) {
        SendMessageCommand command = new SendMessageCommand();
        command.setReceiverId(dto.getReceiverId());
        command.setClientMessageId(dto.getClientMessageId());
        command.setMessageType(dto.getMessageType());
        command.setContent(dto.getContent());
        return command;
    }
}
