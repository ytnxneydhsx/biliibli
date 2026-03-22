package com.bilibili.upload.video.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class VideoUploadStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uploadId;

    private String objectKey;

    private Integer chunkSize;

    private Integer totalChunks;

    private Integer uploadedPartCount;

    private List<Integer> uploadedParts;

    private Integer status;

    private Boolean completed;

    private String expireTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long videoId;

    private String videoUrl;
}
