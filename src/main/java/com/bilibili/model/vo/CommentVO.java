package com.bilibili.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论展示 VO
 * 支持嵌套结构（二级评论）和个性化动态字段（isLiked）
 */
@Data
public class CommentVO implements Serializable {
    private static final long serialVersionUID = 1L;

    // 基础信息 (来自 t_comment)
    private Long id;
    private String content;
    private Long likeCount;
    private LocalDateTime createTime;

    // 作者信息 (来自 t_user_info)
    private String nickname;
    private String avatarUrl;

    // 动态信息 (后端逻辑聚合)
    private Boolean isLiked;

    // 层级信息 (楼中楼预览)
    private List<CommentVO> childComments;
}
