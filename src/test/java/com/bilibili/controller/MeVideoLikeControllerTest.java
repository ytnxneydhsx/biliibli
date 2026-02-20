package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.service.VideoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MeVideoLikeControllerTest {

    @Mock
    private VideoService videoService;

    @InjectMocks
    private MeVideoLikeController meVideoLikeController;

    @Test
    public void likeVideo_shouldUseCurrentUid() {
        meVideoLikeController.likeVideo(new AuthenticatedUser(1001L), 200L);
        verify(videoService, times(1)).likeVideo(eq(1001L), eq(200L));
    }

    @Test
    public void unlikeVideo_shouldUseCurrentUid() {
        meVideoLikeController.unlikeVideo(new AuthenticatedUser(1001L), 200L);
        verify(videoService, times(1)).unlikeVideo(eq(1001L), eq(200L));
    }
}
