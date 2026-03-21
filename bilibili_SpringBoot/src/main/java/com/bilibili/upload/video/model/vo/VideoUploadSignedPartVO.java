package com.bilibili.upload.video.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class VideoUploadSignedPartVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer partNumber;

    private String uploadUrl;
}
