package com.bilibili.im.moderation.controller;

import com.bilibili.common.result.Result;
import com.bilibili.im.moderation.model.dto.CreateSensitiveWordDTO;
import com.bilibili.im.moderation.model.dto.UpdateSensitiveWordDTO;
import com.bilibili.im.moderation.model.vo.SensitiveWordVO;
import com.bilibili.im.moderation.service.SensitiveWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/me/im/sensitive-words")
@PreAuthorize("isAuthenticated()")
@Tag(name = "IM Sensitive Word", description = "IM sensitive word management APIs")
public class ImSensitiveWordController {

    private final SensitiveWordService sensitiveWordService;

    public ImSensitiveWordController(SensitiveWordService sensitiveWordService) {
        this.sensitiveWordService = sensitiveWordService;
    }

    @PostMapping
    @Operation(summary = "Create a sensitive word")
    public Result<Long> createSensitiveWord(@Valid @RequestBody CreateSensitiveWordDTO dto) {
        return Result.success(sensitiveWordService.createSensitiveWord(dto));
    }

    @GetMapping
    @Operation(summary = "List all sensitive words")
    public Result<List<SensitiveWordVO>> listSensitiveWords() {
        return Result.success(sensitiveWordService.listSensitiveWords());
    }

    @GetMapping("/active")
    @Operation(summary = "List active sensitive words")
    public Result<List<SensitiveWordVO>> listActiveSensitiveWords() {
        return Result.success(sensitiveWordService.listActiveSensitiveWords());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a sensitive word")
    public Result<Void> updateSensitiveWord(
            @PathVariable("id")
            @NotNull(message = "id cannot be null")
            @Positive(message = "id must be positive") Long id,
            @Valid @RequestBody UpdateSensitiveWordDTO dto) {
        sensitiveWordService.updateSensitiveWord(id, dto);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a sensitive word")
    public Result<Void> deleteSensitiveWord(
            @PathVariable("id")
            @NotNull(message = "id cannot be null")
            @Positive(message = "id must be positive") Long id) {
        sensitiveWordService.deleteSensitiveWord(id);
        return Result.success(null);
    }
}
