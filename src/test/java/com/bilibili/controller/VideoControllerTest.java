package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.model.vo.VideoDetailVO;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.service.VideoService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VideoControllerTest {

    @Mock
    private VideoService videoService;

    @InjectMocks
    private VideoController videoController;

    @Test
    public void listVideos_shouldDelegateToService() {
        VideoVO item = new VideoVO();
        item.setId(7L);
        item.setTitle("home");
        List<VideoVO> mockedList = Collections.singletonList(item);

        when(videoService.listHomepageVideos(eq(null), eq(1), eq(10)))
                .thenReturn(mockedList);

        List<VideoVO> result = videoController.listVideos(1, 10).getData();

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Long.valueOf(7L), result.get(0).getId());
        verify(videoService, times(1))
                .listHomepageVideos(eq(null), eq(1), eq(10));
    }

    @Test
    public void searchVideos_shouldDelegateToService() {
        VideoVO item = new VideoVO();
        item.setId(8L);
        item.setTitle("java");
        List<VideoVO> mockedList = Collections.singletonList(item);

        when(videoService.searchVideos(eq("java"), eq(2), eq(20))).thenReturn(mockedList);

        List<VideoVO> result = videoController.searchVideos("java", 2, 20).getData();

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Long.valueOf(8L), result.get(0).getId());
        verify(videoService, times(1)).searchVideos(eq("java"), eq(2), eq(20));
    }

    @Test
    public void getVideoDetail_shouldPassCurrentUid() {
        VideoDetailVO detail = new VideoDetailVO();
        detail.setId(99L);
        when(videoService.getVideoDetail(eq(99L), eq(1001L))).thenReturn(detail);

        VideoDetailVO result = videoController
                .getVideoDetail(99L, new AuthenticatedUser(1001L))
                .getData();

        Assert.assertEquals(Long.valueOf(99L), result.getId());
        verify(videoService, times(1)).getVideoDetail(eq(99L), eq(1001L));
    }

    @Test
    public void increaseViewCount_shouldDelegateToService() {
        videoController.increaseViewCount(88L);
        verify(videoService, times(1)).increaseViewCount(eq(88L));
    }
}
