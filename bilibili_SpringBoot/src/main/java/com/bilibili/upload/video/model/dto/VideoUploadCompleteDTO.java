package com.bilibili.upload.video.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class VideoUploadCompleteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;

    private String description;

    private String coverUrl;

    private Long duration;

    private List<VideoUploadPartETagDTO> parts;
}
