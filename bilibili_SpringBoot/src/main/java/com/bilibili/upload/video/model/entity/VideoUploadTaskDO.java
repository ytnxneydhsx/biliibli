package com.bilibili.upload.video.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_video_upload_task")
public class VideoUploadTaskDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String uploadId;

    private Long userId;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private Integer chunkSize;

    private Integer totalChunks;

    // 0=UPLOADING, 1=COMPLETING, 2=DONE, 3=EXPIRED, 4=FAILED, 5=CANCELLED
    private Integer status;

    private String objectKey;

    private String multipartUploadId;

    private Long finalVideoId;

    private String finalVideoUrl;

    private String errorMsg;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
