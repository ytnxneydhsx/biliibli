package com.bilibili.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频列表展示 VO
 * 专门用于首页、搜索列表等高频场景。
 * 移除了 description 等大字段，仅保留核心展示信息以提升查询性能。
 */
@Data
public class VideoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    // 视频核心字段
    private Long id;
    private String title;
    private String coverUrl;
    private Long viewCount;
    private Long duration;
    private LocalDateTime createTime;

    // 作者关联字段 (跨表映射)
    private String nickname;
}
