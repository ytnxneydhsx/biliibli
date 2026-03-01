package com.bilibili.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class VideoUploadCompleteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;

    private String description;

    private String coverUrl;

    private Long duration;
}
