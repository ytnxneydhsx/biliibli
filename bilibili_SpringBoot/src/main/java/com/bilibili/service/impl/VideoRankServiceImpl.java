package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.config.redis.RedisViewCacheKeys;
import com.bilibili.config.redis.RedisViewCacheTuning;
import com.bilibili.mapper.VideoMapper;
import com.bilibili.model.dto.PageQueryDTO;
import com.bilibili.model.vo.VideoRankVO;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.service.VideoRankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class VideoRankServiceImpl implements VideoRankService {

    private final StringRedisTemplate stringRedisTemplate;
    private final VideoMapper videoMapper;

    @Autowired
    public VideoRankServiceImpl(StringRedisTemplate stringRedisTemplate,
                                VideoMapper videoMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.videoMapper = videoMapper;
    }

    @Override
    public void increaseVideoViewScore(Long videoId, long delta) {
        if (videoId == null || videoId <= 0 || delta <= 0) {
            return;
        }
        try {
            String deltaKey = buildDeltaKey(videoId);
            ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
            valueOps.increment(deltaKey, delta);
            stringRedisTemplate.expire(deltaKey, RedisViewCacheTuning.VIDEO_VIEW_DELTA_EXPIRE_SECONDS, TimeUnit.SECONDS);
            stringRedisTemplate.opsForSet().add(RedisViewCacheKeys.VIDEO_VIEW_DIRTY_KEY, String.valueOf(videoId));

            stringRedisTemplate.opsForZSet()
                    .incrementScore(RedisViewCacheKeys.VIDEO_VIEW_RANK_KEY, String.valueOf(videoId), delta);
        } catch (Exception ex) {
            // Redis failure should not break the main request path.
        }
    }

    @Override
    public IPage<VideoRankVO> listVideoViewRank(PageQueryDTO pageQuery) {
        PageQueryDTO query = pageQuery == null ? new PageQueryDTO() : pageQuery;
        int normalizedPageNo = query.normalizedPageNo();
        int normalizedPageSize = query.normalizedPageSize();
        long start = (long) (normalizedPageNo - 1) * normalizedPageSize;
        long end = start + normalizedPageSize - 1;

        try {
            long total = queryRankTotal();
            Set<ZSetOperations.TypedTuple<String>> tuples = fetchByScoreDesc(start, end);
            if (tuples == null || tuples.isEmpty()) {
                return toPage(Collections.emptyList(), normalizedPageNo, normalizedPageSize, total);
            }
            List<VideoRankVO> records = buildRankResultFromRedis(tuples, start);
            return toPage(records, normalizedPageNo, normalizedPageSize, total);
        } catch (Exception ex) {
            return toPage(Collections.emptyList(), normalizedPageNo, normalizedPageSize, 0L);
        }
    }

    private Set<ZSetOperations.TypedTuple<String>> fetchByScoreDesc(long start, long end) {
        return stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisViewCacheKeys.VIDEO_VIEW_RANK_KEY, start, end);
    }

    private List<VideoRankVO> buildRankResultFromRedis(Set<ZSetOperations.TypedTuple<String>> tuples, long start) {
        List<Long> orderedVideoIds = new ArrayList<>();
        Map<Long, Double> scoreMap = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple == null || tuple.getValue() == null) {
                continue;
            }
            Long videoId = parseLong(tuple.getValue());
            if (videoId == null) {
                continue;
            }
            orderedVideoIds.add(videoId);
            Double score = tuple.getScore();
            scoreMap.put(videoId, score == null ? 0D : score);
        }
        if (orderedVideoIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<VideoVO> videos = videoMapper.selectPublishedVideosByIds(orderedVideoIds);
        if (videos == null || videos.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, VideoVO> videoMap = new LinkedHashMap<>();
        for (VideoVO video : videos) {
            if (video.getId() != null) {
                videoMap.put(video.getId(), video);
            }
        }

        List<VideoRankVO> result = new ArrayList<>();
        int rank = (int) start + 1;
        for (Long videoId : orderedVideoIds) {
            VideoVO video = videoMap.get(videoId);
            if (video == null) {
                stringRedisTemplate.opsForZSet().remove(RedisViewCacheKeys.VIDEO_VIEW_RANK_KEY, String.valueOf(videoId));
                continue;
            }
            result.add(toRankVO(video, scoreMap.get(videoId), rank));
            rank++;
        }
        return result;
    }

    private VideoRankVO toRankVO(VideoVO video, Double score, Integer rank) {
        VideoRankVO vo = new VideoRankVO();
        vo.setRank(rank);
        vo.setScore(score == null ? 0D : score);
        vo.setId(video.getId());
        vo.setAuthorUid(video.getAuthorUid());
        vo.setTitle(video.getTitle());
        vo.setCoverUrl(video.getCoverUrl());
        vo.setViewCount(video.getViewCount());
        vo.setDuration(video.getDuration());
        vo.setCreateTime(video.getCreateTime());
        vo.setNickname(video.getNickname());
        return vo;
    }

    private static Long parseLong(String rawValue) {
        try {
            return Long.valueOf(rawValue);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String buildDeltaKey(Long videoId) {
        return RedisViewCacheKeys.buildVideoViewDeltaKey(videoId);
    }

    private long queryRankTotal() {
        Long redisTotal = stringRedisTemplate.opsForZSet().zCard(RedisViewCacheKeys.VIDEO_VIEW_RANK_KEY);
        return redisTotal == null ? 0L : redisTotal;
    }

    private IPage<VideoRankVO> toPage(List<VideoRankVO> records, int pageNo, int pageSize, long total) {
        Page<VideoRankVO> page = new Page<>(pageNo, pageSize, total);
        page.setRecords(records == null ? Collections.emptyList() : records);
        return page;
    }
}
