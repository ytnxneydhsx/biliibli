package com.bilibili.upload.model.vo;

import java.io.Serializable;
import java.util.List;

public class VideoUploadStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uploadId;

    private Integer totalChunks;

    private Integer uploadedChunkCount;

    private List<Integer> uploadedChunks;

    private Boolean completed;

    private String expireTime;

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    public Integer getUploadedChunkCount() {
        return uploadedChunkCount;
    }

    public void setUploadedChunkCount(Integer uploadedChunkCount) {
        this.uploadedChunkCount = uploadedChunkCount;
    }

    public List<Integer> getUploadedChunks() {
        return uploadedChunks;
    }

    public void setUploadedChunks(List<Integer> uploadedChunks) {
        this.uploadedChunks = uploadedChunks;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }
}
