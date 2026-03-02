package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.model.vo.VideoDetailVO;
import com.bilibili.model.vo.VideoRankVO;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.service.VideoRankService;
import com.bilibili.service.VideoService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VideoAppServiceImplTest {

    @Mock
    private VideoService videoService;
    @Mock
    private VideoRankService videoRankService;

    @InjectMocks
    private VideoAppServiceImpl videoAppService;

    @Test
    public void listVideos_shouldDelegateToVideoService() {
        VideoVO vo = new VideoVO();
        vo.setId(1L);
        IPage<VideoVO> mockedPage = new Page<>(1, 10, 1);
        mockedPage.setRecords(Collections.singletonList(vo));
        when(videoService.listHomepageVideos(eq(null), eq(1), eq(10)))
                .thenReturn(mockedPage);

        IPage<VideoVO> result = videoAppService.listVideos(1, 10);

        Assert.assertEquals(1, result.getRecords().size());
        verify(videoService, times(1)).listHomepageVideos(eq(null), eq(1), eq(10));
    }

    @Test
    public void listVideoRank_shouldDelegateToRankService() {
        VideoRankVO vo = new VideoRankVO();
        vo.setId(10L);
        IPage<VideoRankVO> mockedPage = new Page<>(1, 10, 1);
        mockedPage.setRecords(Collections.singletonList(vo));
        when(videoRankService.listVideoViewRank(eq(1), eq(10)))
                .thenReturn(mockedPage);

        IPage<VideoRankVO> result = videoAppService.listVideoRank(1, 10);

        Assert.assertEquals(1, result.getRecords().size());
        verify(videoRankService, times(1)).listVideoViewRank(eq(1), eq(10));
    }

    @Test
    public void increaseViewCount_shouldOrchestrateTwoServices() {
        videoAppService.increaseViewCount(88L);

        verify(videoService, times(1)).validateViewableVideo(eq(88L));
        verify(videoRankService, times(1)).increaseVideoViewScore(eq(88L), eq(1L));
    }

    @Test
    public void getVideoDetail_shouldDelegateToVideoService() {
        VideoDetailVO vo = new VideoDetailVO();
        vo.setId(99L);
        when(videoService.getVideoDetail(eq(99L), eq(1001L))).thenReturn(vo);

        VideoDetailVO result = videoAppService.getVideoDetail(99L, 1001L);

        Assert.assertEquals(Long.valueOf(99L), result.getId());
        verify(videoService, times(1)).getVideoDetail(eq(99L), eq(1001L));
    }
}
