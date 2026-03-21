package com.bilibili.upload.video.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class VideoUploadInitVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uploadId;

    private Integer chunkSize;

    private Integer totalChunks;

    private String objectKey;

    private String expireTime;
}
