package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.config.redis.RedisViewCacheKeys;
import com.bilibili.config.redis.RedisViewCacheTuning;
import com.bilibili.mapper.VideoMapper;
import com.bilibili.model.entity.VideoDO;
import com.bilibili.model.vo.VideoRankVO;
import com.bilibili.model.vo.VideoVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VideoRankServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private VideoMapper videoMapper;
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private VideoRankServiceImpl videoRankService;

    @Test
    public void increaseVideoViewScore_valid_shouldIncrementZSetScore() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        videoRankService.increaseVideoViewScore(100L, 1L);

        verify(valueOperations, times(1))
                .increment(eq(RedisViewCacheKeys.buildVideoViewDeltaKey(100L)), eq(1L));
        verify(stringRedisTemplate, times(1))
                .expire(eq(RedisViewCacheKeys.buildVideoViewDeltaKey(100L)),
                        eq(RedisViewCacheTuning.VIDEO_VIEW_DELTA_EXPIRE_SECONDS),
                        eq(TimeUnit.SECONDS));
        verify(setOperations, times(1))
                .add(eq(RedisViewCacheKeys.VIDEO_VIEW_DIRTY_KEY), eq("100"));
        verify(zSetOperations, times(1))
                .incrementScore(eq(RedisViewCacheKeys.VIDEO_VIEW_RANK_KEY), eq("100"), eq(1D));
    }

    @Test
    public void increaseVideoViewScore_invalid_shouldSkip() {
        videoRankService.increaseVideoViewScore(0L, 1L);
        verify(stringRedisTemplate, never()).opsForValue();
        verify(stringRedisTemplate, never()).opsForZSet();
    }

    @Test
    public void listVideoViewRank_redisHit_shouldReturnOrderedResult() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard(eq(RedisViewCacheKeys.VIDEO_VIEW_RANK_KEY))).thenReturn(2L);
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(new DefaultTypedTuple<>("2", 20D));
        tuples.add(new DefaultTypedTuple<>("1", 10D));
        when(zSetOperations.reverseRangeWithScores(eq(RedisViewCacheKeys.VIDEO_VIEW_RANK_KEY), eq(0L), eq(9L)))
                .thenReturn(tuples);

        VideoVO video1 = new VideoVO();
        video1.setId(1L);
        video1.setTitle("video-1");
        video1.setViewCount(100L);

        VideoVO video2 = new VideoVO();
        video2.setId(2L);
        video2.setTitle("video-2");
        video2.setViewCount(200L);

        when(videoMapper.selectPublishedVideosByIds(eq(Arrays.asList(2L, 1L))))
                .thenReturn(Arrays.asList(video1, video2));

        IPage<VideoRankVO> result = videoRankService.listVideoViewRank(1, 10);

        Assert.assertEquals(2, result.getRecords().size());
        Assert.assertEquals(Long.valueOf(2L), result.getRecords().get(0).getId());
        Assert.assertEquals(Integer.valueOf(1), result.getRecords().get(0).getRank());
        Assert.assertEquals(Double.valueOf(20D), result.getRecords().get(0).getScore());
        Assert.assertEquals(Long.valueOf(1L), result.getRecords().get(1).getId());
        Assert.assertEquals(Integer.valueOf(2), result.getRecords().get(1).getRank());
    }

    @Test
    public void listVideoViewRank_redisEmpty_shouldFallbackToMySql() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard(eq(RedisViewCacheKeys.VIDEO_VIEW_RANK_KEY))).thenReturn(0L);
        when(videoMapper.selectCount(any())).thenReturn(1L);
        when(zSetOperations.reverseRangeWithScores(eq(RedisViewCacheKeys.VIDEO_VIEW_RANK_KEY), eq(0L), eq(9L)))
                .thenReturn(Collections.emptySet(), Collections.emptySet());
        IPage<VideoVO> warmupPage = new Page<>(1, RedisViewCacheTuning.VIDEO_VIEW_RANK_WARMUP_LIMIT, 0);
        warmupPage.setRecords(Collections.emptyList());
        when(videoMapper.selectPublishedVideosByViewCount(argThat(page -> page != null
                && page.getCurrent() == 1
                && page.getSize() == RedisViewCacheTuning.VIDEO_VIEW_RANK_WARMUP_LIMIT)))
                .thenReturn(warmupPage);

        VideoVO video = new VideoVO();
        video.setId(10L);
        video.setViewCount(321L);
        IPage<VideoVO> fallbackPage = new Page<>(1, 10, 1);
        fallbackPage.setRecords(Collections.singletonList(video));
        when(videoMapper.selectPublishedVideosByViewCount(argThat(page -> page != null
                && page.getCurrent() == 1
                && page.getSize() == 10)))
                .thenReturn(fallbackPage);

        IPage<VideoRankVO> result = videoRankService.listVideoViewRank(1, 10);

        Assert.assertEquals(1, result.getRecords().size());
        Assert.assertEquals(Long.valueOf(10L), result.getRecords().get(0).getId());
        Assert.assertEquals(Integer.valueOf(1), result.getRecords().get(0).getRank());
        Assert.assertEquals(Double.valueOf(321D), result.getRecords().get(0).getScore());
    }
}
