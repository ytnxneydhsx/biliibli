package com.bilibili.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 弹幕实体 (映射 t_danmaku)
 */
@Data
@TableName("t_danmaku")
public class DanmakuDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long videoId;

    private Long userId;

    private String content;

    /**
     * 在视频中出现的时刻 (单位：毫秒)
     * 保证弹幕在大流量下依然能丝滑错位展示
     */
    private Long showTime;

    private Long likeCount;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
