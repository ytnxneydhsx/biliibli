package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.model.dto.VideoUploadCompleteDTO;
import com.bilibili.model.dto.VideoUploadInitDTO;
import com.bilibili.model.vo.VideoUploadCompleteVO;
import com.bilibili.model.vo.VideoUploadInitVO;
import com.bilibili.model.vo.VideoUploadStatusVO;
import com.bilibili.service.VideoUploadService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MeVideoUploadControllerTest {

    @Mock
    private VideoUploadService videoUploadService;

    @InjectMocks
    private MeVideoUploadController meVideoUploadController;

    @Test
    public void initUpload_shouldUseCurrentUid() {
        VideoUploadInitDTO dto = new VideoUploadInitDTO();
        dto.setFileName("demo.mp4");
        VideoUploadInitVO vo = new VideoUploadInitVO();
        vo.setUploadId("u1");
        when(videoUploadService.initUpload(eq(1001L), eq(dto))).thenReturn(vo);

        VideoUploadInitVO result = meVideoUploadController
                .initUpload(new AuthenticatedUser(1001L), dto)
                .getData();

        Assert.assertEquals("u1", result.getUploadId());
        verify(videoUploadService, times(1)).initUpload(eq(1001L), eq(dto));
    }

    @Test
    public void uploadChunk_shouldPassAllParams() {
        MockMultipartFile file = new MockMultipartFile("file", "0.part", "application/octet-stream", new byte[]{1, 2, 3});

        meVideoUploadController.uploadChunk(new AuthenticatedUser(1001L), "u1", 0, file);

        verify(videoUploadService, times(1))
                .uploadChunk(eq(1001L), eq("u1"), eq(0), eq(file));
    }

    @Test
    public void getUploadStatus_shouldUseCurrentUid() {
        VideoUploadStatusVO vo = new VideoUploadStatusVO();
        vo.setUploadId("u1");
        when(videoUploadService.getUploadStatus(eq(1001L), eq("u1"))).thenReturn(vo);

        VideoUploadStatusVO result = meVideoUploadController
                .getUploadStatus(new AuthenticatedUser(1001L), "u1")
                .getData();

        Assert.assertEquals("u1", result.getUploadId());
        verify(videoUploadService, times(1)).getUploadStatus(eq(1001L), eq("u1"));
    }

    @Test
    public void completeUpload_shouldUseCurrentUid() {
        VideoUploadCompleteDTO dto = new VideoUploadCompleteDTO();
        dto.setTitle("t");
        VideoUploadCompleteVO vo = new VideoUploadCompleteVO();
        vo.setUploadId("u1");
        vo.setVideoId(200L);
        when(videoUploadService.completeUpload(eq(1001L), eq("u1"), eq(dto))).thenReturn(vo);

        VideoUploadCompleteVO result = meVideoUploadController
                .completeUpload(new AuthenticatedUser(1001L), "u1", dto)
                .getData();

        Assert.assertEquals("u1", result.getUploadId());
        Assert.assertEquals(Long.valueOf(200L), result.getVideoId());
        verify(videoUploadService, times(1))
                .completeUpload(eq(1001L), eq("u1"), eq(dto));
    }
}

