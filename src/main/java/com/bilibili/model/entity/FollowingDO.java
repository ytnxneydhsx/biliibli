package com.bilibili.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 关注关系实体 (映射 t_following)
 */
@Data
@TableName("t_following")
public class FollowingDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 发起关注的用户 ID
     */
    private Long userId;

    /**
     * 被关注的用户 ID
     */
    private Long followingUserId;

    /**
     * 关注状态 (0:关注中, 1:已取关)
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
