package com.bilibili.upload.video.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class VideoUploadPartSignVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uploadId;

    private String expireTime;

    private List<VideoUploadSignedPartVO> parts;
}
