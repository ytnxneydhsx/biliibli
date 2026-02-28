package com.bilibili.controller;

import com.bilibili.common.exception.GlobalExceptionHandler;
import com.bilibili.controller.support.TestAuthenticatedUserArgumentResolver;
import com.bilibili.model.vo.VideoRankVO;
import com.bilibili.model.vo.VideoDetailVO;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.service.VideoAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class VideoControllerTest {

    @Mock
    private VideoAppService videoAppService;

    @InjectMocks
    private VideoController videoController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(videoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new TestAuthenticatedUserArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void listVideos_shouldDelegateToService() throws Exception {
        VideoVO item = new VideoVO();
        item.setId(7L);
        item.setTitle("home");
        List<VideoVO> mockedList = Collections.singletonList(item);

        when(videoAppService.listVideos(eq(1), eq(10)))
                .thenReturn(mockedList);

        mockMvc.perform(get("/videos")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(7L));

        verify(videoAppService, times(1)).listVideos(eq(1), eq(10));
    }

    @Test
    public void getVideoDetail_shouldPassCurrentUid() throws Exception {
        VideoDetailVO detail = new VideoDetailVO();
        detail.setId(99L);
        when(videoAppService.getVideoDetail(eq(99L), eq(1001L))).thenReturn(detail);

        mockMvc.perform(get("/videos/99")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(99L));

        verify(videoAppService, times(1)).getVideoDetail(eq(99L), eq(1001L));
    }

    @Test
    public void listVideoRank_shouldDelegateToService() throws Exception {
        VideoRankVO item = new VideoRankVO();
        item.setRank(1);
        item.setId(100L);
        item.setScore(1234D);

        when(videoAppService.listVideoRank(eq(1), eq(10)))
                .thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/videos/rank")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].rank").value(1));

        verify(videoAppService, times(1)).listVideoRank(eq(1), eq(10));
    }

    @Test
    public void increaseViewCount_shouldDelegateToService() throws Exception {
        mockMvc.perform(post("/videos/88/views"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(videoAppService, times(1)).increaseViewCount(eq(88L));
    }
}
