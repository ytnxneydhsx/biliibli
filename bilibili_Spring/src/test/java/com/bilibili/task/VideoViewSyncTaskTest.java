package com.bilibili.task;

import com.bilibili.config.redis.RedisViewCacheKeys;
import com.bilibili.mapper.VideoMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VideoViewSyncTaskTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private VideoMapper videoMapper;

    @InjectMocks
    private VideoViewSyncTask videoViewSyncTask;

    @Test
    public void syncViewDeltaToMySql_shouldFlushDeltaAndClearKey() {
        String dirtyKey = RedisViewCacheKeys.VIDEO_VIEW_DIRTY_KEY;
        String videoId = "100";
        String deltaKey = RedisViewCacheKeys.buildVideoViewDeltaKey(100L);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(eq(dirtyKey))).thenReturn(Collections.singleton(videoId));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(eq(deltaKey))).thenReturn("5");
        when(videoMapper.updateViewCountByDelta(eq(100L), eq(5L))).thenReturn(1);
        when(valueOperations.increment(eq(deltaKey), eq(-5L))).thenReturn(0L);

        videoViewSyncTask.syncViewDeltaToMySql();

        verify(videoMapper, times(1)).updateViewCountByDelta(eq(100L), eq(5L));
        verify(stringRedisTemplate, times(1)).delete(eq(deltaKey));
        verify(setOperations, times(1)).remove(eq(dirtyKey), eq(videoId));
    }

    @Test
    public void syncViewDeltaToMySql_invalidKey_shouldSkip() {
        String dirtyKey = RedisViewCacheKeys.VIDEO_VIEW_DIRTY_KEY;
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(eq(dirtyKey))).thenReturn(Collections.singleton("abc"));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        videoViewSyncTask.syncViewDeltaToMySql();

        verify(videoMapper, never()).updateViewCountByDelta(any(), any());
        verify(setOperations, times(1)).remove(eq(dirtyKey), eq("abc"));
    }
}
