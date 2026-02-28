package com.bilibili.controller;

import com.bilibili.common.exception.GlobalExceptionHandler;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.service.VideoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class UserVideoControllerTest {

    @Mock
    private VideoService videoService;

    @InjectMocks
    private UserVideoController userVideoController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(userVideoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void listPublishedVideos_shouldUsePathUid() throws Exception {
        VideoVO item = new VideoVO();
        item.setId(1L);
        item.setTitle("demo");
        List<VideoVO> mockedList = Collections.singletonList(item);

        when(videoService.listPublishedVideos(1001L, "test", 2, 20))
                .thenReturn(mockedList);

        MvcResult mvcResult = mockMvc.perform(get("/users/1001/videos")
                        .param("title", "test")
                        .param("pageNo", "2")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(0, objectMapper.readTree(mvcResult.getResponse().getContentAsString()).get("code").asInt());
        assertEquals(1L, objectMapper.readTree(mvcResult.getResponse().getContentAsString()).get("data").get(0).get("id").asLong());
        verify(videoService, times(1)).listPublishedVideos(1001L, "test", 2, 20);
    }

    @Test
    public void listPublishedVideos_withoutParams_shouldPassNulls() throws Exception {
        when(videoService.listPublishedVideos(1001L, null, null, null))
                .thenReturn(Collections.emptyList());

        MvcResult mvcResult = mockMvc.perform(get("/users/1001/videos"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(0, objectMapper.readTree(mvcResult.getResponse().getContentAsString()).get("code").asInt());
        verify(videoService, times(1)).listPublishedVideos(1001L, null, null, null);
    }
}
