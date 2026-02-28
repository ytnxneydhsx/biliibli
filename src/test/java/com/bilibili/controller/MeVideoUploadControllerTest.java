package com.bilibili.controller;

import com.bilibili.common.exception.GlobalExceptionHandler;
import com.bilibili.controller.support.TestAuthenticatedUserArgumentResolver;
import com.bilibili.model.dto.VideoUploadCompleteDTO;
import com.bilibili.model.dto.VideoUploadInitDTO;
import com.bilibili.model.vo.VideoUploadCompleteVO;
import com.bilibili.model.vo.VideoUploadInitVO;
import com.bilibili.model.vo.VideoUploadStatusVO;
import com.bilibili.service.VideoUploadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class MeVideoUploadControllerTest {

    @Mock
    private VideoUploadService videoUploadService;

    @InjectMocks
    private MeVideoUploadController meVideoUploadController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(meVideoUploadController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new TestAuthenticatedUserArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void initUpload_shouldUseCurrentUid() throws Exception {
        VideoUploadInitDTO dto = new VideoUploadInitDTO();
        dto.setFileName("demo.mp4");
        VideoUploadInitVO vo = new VideoUploadInitVO();
        vo.setUploadId("u1");
        when(videoUploadService.initUpload(eq(1001L), any(VideoUploadInitDTO.class))).thenReturn(vo);

        mockMvc.perform(post("/me/videos/uploads/init-session")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.uploadId").value("u1"));

        verify(videoUploadService, times(1)).initUpload(eq(1001L), any(VideoUploadInitDTO.class));
    }

    @Test
    public void uploadChunk_shouldPassAllParams() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "0.part", "application/octet-stream", new byte[]{1, 2, 3});

        MockHttpServletRequestBuilder request = multipart("/me/videos/uploads/{uploadId}/chunks/{index}", "u1", 0)
                .file(file)
                .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001");
        request.with(r -> {
            r.setMethod("PUT");
            return r;
        });

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(videoUploadService, times(1))
                .uploadChunk(eq(1001L), eq("u1"), eq(0), any(MultipartFile.class));
    }

    @Test
    public void getUploadStatus_shouldUseCurrentUid() throws Exception {
        VideoUploadStatusVO vo = new VideoUploadStatusVO();
        vo.setUploadId("u1");
        when(videoUploadService.getUploadStatus(eq(1001L), eq("u1"))).thenReturn(vo);

        mockMvc.perform(get("/me/videos/uploads/u1")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.uploadId").value("u1"));

        verify(videoUploadService, times(1)).getUploadStatus(eq(1001L), eq("u1"));
    }

    @Test
    public void completeUpload_shouldUseCurrentUid() throws Exception {
        VideoUploadCompleteDTO dto = new VideoUploadCompleteDTO();
        dto.setTitle("t");
        VideoUploadCompleteVO vo = new VideoUploadCompleteVO();
        vo.setUploadId("u1");
        vo.setVideoId(200L);
        when(videoUploadService.completeUpload(eq(1001L), eq("u1"), any(VideoUploadCompleteDTO.class))).thenReturn(vo);

        mockMvc.perform(post("/me/videos/uploads/u1/complete")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.uploadId").value("u1"))
                .andExpect(jsonPath("$.data.videoId").value(200L));

        verify(videoUploadService, times(1))
                .completeUpload(eq(1001L), eq("u1"), any(VideoUploadCompleteDTO.class));
    }
}
