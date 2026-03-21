package com.bilibili.upload.video.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class VideoUploadInitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileName;

    private Long totalSize;

    private Integer chunkSize;

    private Integer totalChunks;

    private String contentType;

    private String fileMd5;
}
