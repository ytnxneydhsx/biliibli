package com.bilibili.video.task;

import com.bilibili.config.redis.RedisViewCacheKeys;
import com.bilibili.config.redis.RedisViewCacheTuning;
import com.bilibili.video.mapper.VideoMapper;
import com.bilibili.tool.StringTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
public class VideoViewSyncTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final VideoMapper videoMapper;

    @Autowired
    public VideoViewSyncTask(StringRedisTemplate stringRedisTemplate,
                             VideoMapper videoMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.videoMapper = videoMapper;
    }

    @Scheduled(fixedDelay = RedisViewCacheTuning.VIDEO_VIEW_SYNC_FIXED_DELAY_MS)
    public void syncViewDeltaToMySql() {
        Set<String> dirtyVideoIds = readDirtyVideoIds();
        if (dirtyVideoIds.isEmpty()) {
            return;
        }

        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        SetOperations<String, String> setOps = stringRedisTemplate.opsForSet();
        for (String rawVideoId : dirtyVideoIds) {
            Long videoId = parsePositiveLong(rawVideoId);
            if (videoId == null || videoId <= 0) {
                setOps.remove(RedisViewCacheKeys.VIDEO_VIEW_DIRTY_KEY, rawVideoId);
                continue;
            }

            String deltaKey = buildDeltaKey(videoId);
            String rawValue = valueOps.get(deltaKey);
            Long delta = parsePositiveLong(rawValue);
            if (delta == null || delta <= 0) {
                setOps.remove(RedisViewCacheKeys.VIDEO_VIEW_DIRTY_KEY, rawVideoId);
                continue;
            }

            int rows = videoMapper.updateViewCountByDelta(videoId, delta);
            if (rows != 1) {
                continue;
            }

            Long remain = valueOps.increment(deltaKey, -delta);
            if (remain == null || remain <= 0) {
                stringRedisTemplate.delete(deltaKey);
                setOps.remove(RedisViewCacheKeys.VIDEO_VIEW_DIRTY_KEY, rawVideoId);
            }
        }
    }

    private Set<String> readDirtyVideoIds() {
        Set<String> videoIds = stringRedisTemplate.opsForSet().members(RedisViewCacheKeys.VIDEO_VIEW_DIRTY_KEY);
        if (videoIds == null) {
            return Collections.emptySet();
        }
        return videoIds;
    }

    private static Long parsePositiveLong(String raw) {
        String normalized = StringTool.normalizeOptional(raw);
        if (normalized == null) {
            return null;
        }
        try {
            long value = Long.parseLong(normalized);
            return value <= 0 ? null : value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String buildDeltaKey(Long videoId) {
        return RedisViewCacheKeys.buildVideoViewDeltaKey(videoId);
    }
}
