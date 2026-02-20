package com.bilibili.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO implements Serializable {
    private static final long serialVersionUID = 1L;

    
    private Long id;
    private String content;
    private Long likeCount;
    private LocalDateTime createTime;

    
    private String nickname;
    private String avatarUrl;

    
    private Boolean isLiked;

    
    private List<CommentVO> childComments;
}
