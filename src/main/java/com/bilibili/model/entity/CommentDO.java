package com.bilibili.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论实体 (映射 t_comment)
 */
@Data
@TableName("t_comment")
public class CommentDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long videoId;

    private Long userId;

    private String content;

    /**
     * 父评论 ID
     * 如果为 0 则是一级评论，如果不为 0 则是回复（楼中楼）
     */
    private Long parentId;

    private Long likeCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
