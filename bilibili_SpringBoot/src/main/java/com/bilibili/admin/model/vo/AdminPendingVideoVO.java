package com.bilibili.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AdminPendingVideoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long authorUid;

    private String title;

    private String description;

    private String coverUrl;

    private String videoUrl;

    private Long duration;

    private LocalDateTime createTime;

    /** 提交者昵称 */
    private String nickname;
}
