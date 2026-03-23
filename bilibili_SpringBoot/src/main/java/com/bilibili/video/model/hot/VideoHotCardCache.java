package com.bilibili.video.model.hot;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoHotCardCache {

    private Long id;
    private Long authorUid;
    private String title;
    private String coverUrl;
    private Long viewCount;
    private Long duration;
    private LocalDateTime createTime;
    private String nickname;
    private Long lastViewAt;
    private String scope;
}
