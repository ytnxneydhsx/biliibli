package com.bilibili.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VideoDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String videoUrl;
    private String title;
    private String desc;
    private String coverUrl;
    private Long duration;
    private LocalDateTime uploadDate;

    private AuthorVO author;
    private List<String> tags;

    private Long viewCount;
    private Long likeCount;
    private Long danmakuCount;
    private Long commentCount;

    private Boolean isLiked;
    private Boolean isFollowed;

    @Data
    public static class AuthorVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long uid;
        private String nickname;
        private String avatar;
        private String sign;
    }
}
