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

    // Delay the first rotation so startup bootstrap can hydrate the active slot first.
    @Scheduled(
            fixedDelayString = "#{@videoHotProperties.switchIntervalMillis}",
            initialDelayString = "#{@videoHotProperties.switchIntervalMillis}"
    )
    public void rotate() {
        videoHotRotationService.rotateSlots();
    }
}
