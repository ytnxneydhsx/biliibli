package com.bilibili.upload.video.model.vo;

import java.io.Serializable;

public class VideoUploadCompleteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uploadId;

    private Long videoId;

    private String videoUrl;

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
