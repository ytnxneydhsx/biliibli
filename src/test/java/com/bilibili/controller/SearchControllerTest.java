package com.bilibili.controller;

import com.bilibili.common.exception.GlobalExceptionHandler;
import com.bilibili.controller.support.TestAuthenticatedUserArgumentResolver;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.service.SearchService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class SearchControllerTest {

    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchController searchController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(searchController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new TestAuthenticatedUserArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void searchVideos_withoutLogin_shouldNotRecordHistory() throws Exception {
        VideoVO item = new VideoVO();
        item.setId(8L);
        item.setTitle("java");
        when(searchService.searchVideos(eq("java"), isNull(), eq(2), eq(20)))
                .thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/search/videos")
                        .param("keyword", "java")
                        .param("pageNo", "2")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(8L));

        verify(searchService, times(1)).searchVideos(eq("java"), isNull(), eq(2), eq(20));
        verify(searchService, never()).recordVideoSearchHistory(eq(1001L), eq("java"));
    }

    @Test
    public void searchVideos_withLogin_shouldRecordHistory() throws Exception {
        when(searchService.searchVideos(eq("java"), isNull(), eq(1), eq(10)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/search/videos")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001")
                        .param("keyword", "java")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(searchService, times(1)).recordVideoSearchHistory(eq(1001L), eq("java"));
    }

    @Test
    public void searchVideos_withCategoryOnly_shouldSkipHistory() throws Exception {
        when(searchService.searchVideos(eq(null), eq(12L), eq(1), eq(10)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/search/videos")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001")
                        .param("categoryId", "12")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(searchService, times(1)).searchVideos(eq(null), eq(12L), eq(1), eq(10));
        verify(searchService, never()).recordVideoSearchHistory(eq(1001L), anyString());
    }

    @Test
    public void listVideoHistory_withoutLogin_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/search/videos/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(searchService, never()).listVideoSearchHistory(eq(1001L));
    }

    @Test
    public void listVideoHistory_withLogin_shouldReturnCurrentUserHistory() throws Exception {
        List<String> history = Arrays.asList("java", "spring");
        when(searchService.listVideoSearchHistory(eq(1001L))).thenReturn(history);

        mockMvc.perform(get("/search/videos/history")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0]").value("java"))
                .andExpect(jsonPath("$.data[1]").value("spring"));

        verify(searchService, times(1)).listVideoSearchHistory(eq(1001L));
    }
}
