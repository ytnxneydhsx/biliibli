package com.bilibili.upload.video.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class VideoUploadPartSignDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Integer> partNumbers;
}
