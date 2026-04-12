package com.bilibili.admin.controller;

import com.bilibili.admin.model.dto.AdminVideoReviewDTO;
import com.bilibili.admin.model.vo.AdminPendingVideoVO;
import com.bilibili.admin.service.AdminVideoService;
import com.bilibili.common.result.CursorPageVO;
import com.bilibili.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/videos")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Video", description = "管理员视频审核接口")
public class AdminVideoController {

    private final AdminVideoService adminVideoService;

    public AdminVideoController(AdminVideoService adminVideoService) {
        this.adminVideoService = adminVideoService;
    }

    @GetMapping("/pending")
    @Operation(summary = "查询待审核视频列表（游标分页）")
    public Result<CursorPageVO<AdminPendingVideoVO>> listPendingVideos(
            @Parameter(description = "游标，传上一页最后一条的id，首页不传")
            @RequestParam(required = false) Long cursor) {
        return Result.success(adminVideoService.listPendingVideos(cursor));
    }

    @GetMapping("/deleted")
    @Operation(summary = "查询已删除视频列表（游标分页）")
    public Result<CursorPageVO<AdminPendingVideoVO>> listDeletedVideos(
            @Parameter(description = "游标，传上一页最后一条的id，首页不传")
            @RequestParam(required = false) Long cursor) {
        return Result.success(adminVideoService.listDeletedVideos(cursor));
    }

    @GetMapping("/published")
    @Operation(summary = "查询已上架视频列表（游标分页）")
    public Result<CursorPageVO<AdminPendingVideoVO>> listPublishedVideos(
            @Parameter(description = "游标，传上一页最后一条的id，首页不传")
            @RequestParam(required = false) Long cursor) {
        return Result.success(adminVideoService.listPublishedVideos(cursor));
    }

    @PutMapping("/{videoId}/status")
    @Operation(summary = "审核视频：通过(0)或拒绝(1)")
    public Result<Void> reviewVideo(
            @PathVariable Long videoId,
            @Valid @RequestBody AdminVideoReviewDTO dto) {
        adminVideoService.reviewVideo(videoId, dto.getStatus());
        return Result.success(null);
    }
}
