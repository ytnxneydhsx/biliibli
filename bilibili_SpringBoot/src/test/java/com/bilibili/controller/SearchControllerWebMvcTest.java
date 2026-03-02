package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.model.dto.PageQueryDTO;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.security.JwtTokenService;
import com.bilibili.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc
class SearchControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    void searchVideos_shouldRecordHistoryWhenAuthenticated() throws Exception {
        VideoVO video = new VideoVO();
        video.setId(101L);
        video.setTitle("java");
        when(jwtTokenService.parse("token-1001")).thenReturn(new AuthenticatedUser(1001L));
        when(searchService.searchVideos(eq("java"), eq(null), argThat(query -> query != null
                && Integer.valueOf(1).equals(query.getPageNo())
                && Integer.valueOf(10).equals(query.getPageSize()))))
                .thenReturn(List.of(video));

        mockMvc.perform(get("/search/videos")
                        .param("keyword", "java")
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer token-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(101L))
                .andExpect(jsonPath("$.data[0].title").value("java"));

        verify(searchService).recordVideoSearchHistory(1001L, "java");
        verify(searchService).searchVideos(eq("java"), eq(null), argThat((PageQueryDTO query) -> query != null
                && Integer.valueOf(1).equals(query.getPageNo())
                && Integer.valueOf(10).equals(query.getPageSize())));
    }

    @Test
    void listMyVideoSearchHistory_shouldReturnEmptyWhenAnonymous() throws Exception {
        mockMvc.perform(get("/search/videos/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verifyNoInteractions(searchService);
    }

    @Test
    void listMyVideoSearchHistory_shouldReturnDataWhenAuthenticated() throws Exception {
        when(jwtTokenService.parse("token-1001")).thenReturn(new AuthenticatedUser(1001L));
        when(searchService.listVideoSearchHistory(1001L)).thenReturn(List.of("java", "spring"));

        mockMvc.perform(get("/search/videos/history")
                        .header("Authorization", "Bearer token-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0]").value("java"))
                .andExpect(jsonPath("$.data[1]").value("spring"));

        verify(searchService).listVideoSearchHistory(eq(1001L));
    }
}
