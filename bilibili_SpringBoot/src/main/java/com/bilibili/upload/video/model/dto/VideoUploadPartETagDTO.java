package com.bilibili.upload.video.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class VideoUploadPartETagDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer partNumber;

    private String etag;
}
