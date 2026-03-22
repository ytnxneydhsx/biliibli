package com.bilibili.comment.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long videoId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long uid;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parentId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long rootId;
    private String content;
    private Long likeCount;
    private Integer replyCount;
    private LocalDateTime createTime;

    private String nickname;
    private String avatar;

    private Boolean isLiked;

    private List<CommentVO> childComments;
}
