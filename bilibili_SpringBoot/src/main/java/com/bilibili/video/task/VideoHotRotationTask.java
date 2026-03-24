package com.bilibili.video.task;

import com.bilibili.video.service.hot.VideoHotRotationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VideoHotRotationTask {

    private final VideoHotRotationService videoHotRotationService;

    public VideoHotRotationTask(VideoHotRotationService videoHotRotationService) {
        this.videoHotRotationService = videoHotRotationService;
    }

    @Scheduled(fixedDelayString = "#{@videoHotProperties.switchIntervalMillis}")
    public void rotate() {
        videoHotRotationService.rotateSlots();
    }
}
