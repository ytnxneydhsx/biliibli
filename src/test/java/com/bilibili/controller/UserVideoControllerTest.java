package com.bilibili.controller;

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
public class UserVideoControllerTest {

    @Mock
    private VideoService videoService;

    @InjectMocks
    private UserVideoController userVideoController;

    @Test
    public void listPublishedVideos_shouldUsePathUid() {
        VideoVO item = new VideoVO();
        item.setId(1L);
        item.setTitle("demo");
        List<VideoVO> mockedList = Collections.singletonList(item);

        when(videoService.listPublishedVideos(eq(1001L), eq("test"), eq(2), eq(20)))
                .thenReturn(mockedList);

        List<VideoVO> result = userVideoController
                .listPublishedVideos(1001L, "test", 2, 20)
                .getData();

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Long.valueOf(1L), result.get(0).getId());
        verify(videoService, times(1))
                .listPublishedVideos(eq(1001L), eq("test"), eq(2), eq(20));
    }
}
