package com.bilibili.video.service.hot;

import com.bilibili.config.properties.VideoHotProperties;
import com.bilibili.video.mapper.VideoMapper;
import com.bilibili.video.model.hot.VideoHotCardCache;
import com.bilibili.video.redis.VideoHotRedisRepository;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class VideoHotRotationService {

    private final VideoHotRedisRepository videoHotRedisRepository;
    private final VideoHotFacade videoHotFacade;
    private final VideoMapper videoMapper;
    private final VideoHotProperties videoHotProperties;

    public VideoHotRotationService(VideoHotRedisRepository videoHotRedisRepository,
                                   VideoHotFacade videoHotFacade,
                                   VideoMapper videoMapper,
                                   VideoHotProperties videoHotProperties) {
        this.videoHotRedisRepository = videoHotRedisRepository;
        this.videoHotFacade = videoHotFacade;
        this.videoMapper = videoMapper;
        this.videoHotProperties = videoHotProperties;
    }

    public void rotateSlots() {
        String activeSlot = videoHotRedisRepository.getActiveSlot();
        videoHotRedisRepository.freezeWrites();
        try {
            videoHotRedisRepository.copyActiveToStandby(activeSlot);
            videoHotRedisRepository.setActiveSlot(videoHotRedisRepository.getStandbySlot(activeSlot));
        } finally {
            videoHotRedisRepository.unfreezeWrites();
        }

        videoHotFacade.drainFrozenQueue();
        flushOldSlot(activeSlot);
        videoHotRedisRepository.clearSlot(activeSlot);
    }

    private void flushOldSlot(String oldSlot) {
        Set<Long> dirtyVideoIds = videoHotRedisRepository.readDirtyVideoIds(oldSlot);
        if (dirtyVideoIds.isEmpty()) {
            return;
        }
        int processed = 0;
        for (Long videoId : dirtyVideoIds) {
            VideoHotCardCache card = videoHotRedisRepository.loadCard(oldSlot, videoId);
            if (card == null || card.getViewCount() == null) {
                continue;
            }
            videoMapper.updateViewCountAbsolute(videoId, card.getViewCount());
            processed++;
            if (processed >= videoHotProperties.getFlushBatchSize()) {
                processed = 0;
            }
        }
    }
}
