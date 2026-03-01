package com.bilibili.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_video")
public class VideoDO implements Serializable {

    private static final long serialVersionUID = 1L;

    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    
    private Long userId;

    
    private String title;

    
    private String description;

    
    private String coverUrl;

    
    private String videoUrl;

    
    private Long duration;

    
    private Long viewCount;

    
    private Long likeCount;

    private Long commentCount;

    
    private Integer status;

    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
