package com.bilibili.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class VideoHotProperties {

    @Value("${app.video.rankSize:100}")
    private int rankSize;

    @Value("${app.video.switchIntervalMinutes:5}")
    private int switchIntervalMinutes;

    @Value("${app.video.activeWindowMinutes:5}")
    private int activeWindowMinutes;

    @Value("${app.video.copyBatchSize:100}")
    private int copyBatchSize;

    @Value("${app.video.flushBatchSize:100}")
    private int flushBatchSize;

    public int getRankSize() {
        return Math.max(rankSize, 1);
    }

    public int getSwitchIntervalMinutes() {
        return Math.max(switchIntervalMinutes, 1);
    }

    public int getActiveWindowMinutes() {
        return Math.max(activeWindowMinutes, 1);
    }

    public int getCopyBatchSize() {
        return Math.max(copyBatchSize, 1);
    }

    public int getFlushBatchSize() {
        return Math.max(flushBatchSize, 1);
    }

    public long getSwitchIntervalMillis() {
        return TimeUnit.MINUTES.toMillis(getSwitchIntervalMinutes());
    }

    public long getActiveWindowMillis() {
        return TimeUnit.MINUTES.toMillis(getActiveWindowMinutes());
    }
}
