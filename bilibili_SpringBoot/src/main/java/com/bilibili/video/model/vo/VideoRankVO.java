package com.bilibili.video.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class VideoRankVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer rank;
    private Double score;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long authorUid;
    private String title;
    private String coverUrl;
    private Long viewCount;
    private Long duration;
    private LocalDateTime createTime;
    private String nickname;
}
