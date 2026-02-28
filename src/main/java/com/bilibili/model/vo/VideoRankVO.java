package com.bilibili.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class VideoRankVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer rank;
    private Double score;

    private Long id;
    private Long authorUid;
    private String title;
    private String coverUrl;
    private Long viewCount;
    private Long duration;
    private LocalDateTime createTime;
    private String nickname;
}
