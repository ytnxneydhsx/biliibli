package com.bilibili.admin.controller;

import com.bilibili.admin.service.AdminUserAccessService;
import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - User Access", description = "管理员用户权限管理接口")
public class AdminUserAccessController {

    private final AdminUserAccessService adminUserAccessService;

    public AdminUserAccessController(AdminUserAccessService adminUserAccessService) {
        this.adminUserAccessService = adminUserAccessService;
    }

    @PostMapping("/{userId}/video-business-ban")
    @Operation(summary = "封禁用户的视频业务能力")
    public Result<Void> banVideoBusiness(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("userId")
            @NotNull(message = "userId cannot be null")
            @Positive(message = "userId must be positive") Long userId) {
        adminUserAccessService.banVideoBusiness(
                userId,
                currentUser == null ? null : currentUser.getUid()
        );
        return Result.success(null);
    }

    @DeleteMapping("/{userId}/video-business-ban")
    @Operation(summary = "解禁用户的视频业务能力")
    public Result<Void> unbanVideoBusiness(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("userId")
            @NotNull(message = "userId cannot be null")
            @Positive(message = "userId must be positive") Long userId) {
        adminUserAccessService.unbanVideoBusiness(
                userId,
                currentUser == null ? null : currentUser.getUid()
        );
        return Result.success(null);
    }
}
