package com.bilibili.admin.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class AdminVideoReviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目标状态: 0 = 通过(NORMAL), 1 = 拒绝(DELETED)
     */
    @NotNull(message = "status is required")
    private Integer status;

    /** 审核理由（可选） */
    private String reason;
}
