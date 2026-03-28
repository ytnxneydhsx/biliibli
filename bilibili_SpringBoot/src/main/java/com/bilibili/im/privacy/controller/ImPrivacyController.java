package com.bilibili.im.privacy.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import com.bilibili.im.privacy.model.dto.UpdatePrivateMessagePolicyDTO;
import com.bilibili.im.privacy.model.vo.UserPrivacySettingVO;
import com.bilibili.im.privacy.service.UserPrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/im/privacy")
@PreAuthorize("isAuthenticated()")
@Tag(name = "IM Privacy", description = "Current user IM privacy APIs")
public class ImPrivacyController {

    private final UserPrivacyService userPrivacyService;

    public ImPrivacyController(UserPrivacyService userPrivacyService) {
        this.userPrivacyService = userPrivacyService;
    }

    @GetMapping
    @Operation(summary = "Get my IM privacy setting")
    public Result<UserPrivacySettingVO> getMyPrivacySetting(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return Result.success(userPrivacyService.getPrivacySetting(currentUser.getUid()));
    }

    @PutMapping
    @Operation(summary = "Update my private message policy")
    public Result<Void> updateMyPrivateMessagePolicy(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdatePrivateMessagePolicyDTO dto) {
        userPrivacyService.updatePrivateMessagePolicy(currentUser.getUid(), dto);
        return Result.success(null);
    }
}
