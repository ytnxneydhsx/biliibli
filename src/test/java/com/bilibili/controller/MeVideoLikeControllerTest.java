package com.bilibili.controller;

import com.bilibili.common.exception.GlobalExceptionHandler;
import com.bilibili.controller.support.TestAuthenticatedUserArgumentResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import com.bilibili.service.VideoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class MeVideoLikeControllerTest {

    @Mock
    private VideoService videoService;

    @InjectMocks
    private MeVideoLikeController meVideoLikeController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(meVideoLikeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new TestAuthenticatedUserArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void likeVideo_shouldUseCurrentUid() throws Exception {
        mockMvc.perform(post("/me/videos/200/likes")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(videoService, times(1)).likeVideo(eq(1001L), eq(200L));
    }

    @Test
    public void unlikeVideo_shouldUseCurrentUid() throws Exception {
        mockMvc.perform(delete("/me/videos/200/likes")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(videoService, times(1)).unlikeVideo(eq(1001L), eq(200L));
    }
}
