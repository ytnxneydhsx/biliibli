package com.bilibili.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频实体 (映射数据库表 t_video)
 */
@Data
@TableName("t_video")
public class VideoDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 雪花算法 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 发布者ID
     */
    private Long userId;

    /**
     * 视频标题
     */
    private String title;

    /**
     * 视频介绍 (对应数据库 TEXT 类型)
     */
    private String description;

    /**
     * 封面地址
     */
    private String coverUrl;

    /**
     * 视频源地址
     */
    private String videoUrl;

    /**
     * 视频时长 (秒)
     */
    private Long duration;

    /**
     * 观看数
     */
    private Long viewCount;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 视频状态 (0:正常/发布, 1:审核中, 2:审核失败, 3:删除)
     */
    private Integer status;

    /**
     * 发布时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最后修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
