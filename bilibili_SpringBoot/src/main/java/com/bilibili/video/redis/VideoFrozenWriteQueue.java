package com.bilibili.video.redis;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class VideoFrozenWriteQueue {

    private final Queue<Long> queue = new ConcurrentLinkedQueue<>();

    public void offer(Long videoId) {
        if (videoId != null && videoId > 0) {
            queue.offer(videoId);
        }
    }

    public List<Long> drainAll() {
        List<Long> drained = new ArrayList<>();
        Long videoId;
        while ((videoId = queue.poll()) != null) {
            drained.add(videoId);
        }
        return drained;
    }
}
