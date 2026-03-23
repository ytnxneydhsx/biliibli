package com.bilibili.video.redis.repository;

import com.bilibili.config.properties.VideoHotProperties;
import com.bilibili.video.redis.key.VideoRedisKeys;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VideoHotLuaRepository {

    private final StringRedisTemplate stringRedisTemplate;
    private final VideoHotProperties videoHotProperties;
    private final DefaultRedisScript<Long> increaseViewScript;

    public VideoHotLuaRepository(StringRedisTemplate stringRedisTemplate,
                                 VideoHotProperties videoHotProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.videoHotProperties = videoHotProperties;
        this.increaseViewScript = new DefaultRedisScript<>();
        this.increaseViewScript.setLocation(new ClassPathResource("scripts/redis/increase_video_view.lua"));
        this.increaseViewScript.setResultType(Long.class);
    }

    public Long increaseView(String slot, Long videoId, long nowMillis) {
        return stringRedisTemplate.execute(
                increaseViewScript,
                List.of(
                        VideoRedisKeys.rankKey(slot),
                        VideoRedisKeys.dirtyKey(slot),
                        VideoRedisKeys.cardKey(slot, videoId),
                        VideoRedisKeys.cardIndexKey(slot)
                ),
                String.valueOf(videoId),
                String.valueOf(nowMillis),
                String.valueOf(videoHotProperties.getRankSize()),
                VideoRedisKeys.cardKeyPrefix(slot)
        );
    }
}
