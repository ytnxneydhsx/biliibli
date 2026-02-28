package com.bilibili.service.impl;

import com.bilibili.mapper.VideoMapper;
import com.bilibili.model.vo.VideoVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private VideoMapper videoMapper;

    @InjectMocks
    private SearchServiceImpl searchService;

    @Test
    public void searchVideos_shouldRequireAtLeastOneCondition() {
        IllegalArgumentException ex = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> searchService.searchVideos("   ", null, 1, 10)
        );

        Assert.assertTrue(ex.getMessage().contains("at least one search condition is required"));
    }

    @Test
    public void searchVideos_keywordOnly_shouldFilterByKeywordCandidates() {
        when(videoMapper.selectPublishedVideoIdsByTitle(eq("spring"), eq(200)))
                .thenReturn(Collections.singletonList(100L));

        VideoVO vo = new VideoVO();
        vo.setId(100L);
        when(videoMapper.selectPublishedVideosByIds(eq(Collections.singletonList(100L))))
                .thenReturn(Collections.singletonList(vo));

        List<VideoVO> result = searchService.searchVideos("spring", null, 1, 10);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Long.valueOf(100L), result.get(0).getId());
        verify(videoMapper, times(1)).selectPublishedVideoIdsByTitle(eq("spring"), eq(200));
    }

    @Test
    public void searchVideos_keywordAndCategory_shouldUseIntersection() {
        when(videoMapper.selectPublishedVideoIdsByTitle(eq("java"), eq(200)))
                .thenReturn(Arrays.asList(101L, 102L, 103L));
        when(videoMapper.selectPublishedVideoIdsByCategoryId(eq(9L), eq(200)))
                .thenReturn(Arrays.asList(103L, 102L, 999L));

        VideoVO v102 = new VideoVO();
        v102.setId(102L);
        VideoVO v103 = new VideoVO();
        v103.setId(103L);
        // Service no longer reorders by candidate id list; it follows DB order.
        when(videoMapper.selectPublishedVideosByIds(eq(Arrays.asList(102L, 103L))))
                .thenReturn(Arrays.asList(v103, v102));

        List<VideoVO> result = searchService.searchVideos("java", 9L, 1, 10);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals(Long.valueOf(103L), result.get(0).getId());
        Assert.assertEquals(Long.valueOf(102L), result.get(1).getId());

        verify(videoMapper, times(1))
                .selectPublishedVideoIdsByTitle(eq("java"), eq(200));
        verify(videoMapper, times(1))
                .selectPublishedVideoIdsByCategoryId(eq(9L), eq(200));
    }
}
