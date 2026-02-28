package com.bilibili.controller;

import com.bilibili.common.exception.GlobalExceptionHandler;
import com.bilibili.controller.support.TestAuthenticatedUserArgumentResolver;
import com.bilibili.model.vo.CommentVO;
import com.bilibili.service.CommentService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new TestAuthenticatedUserArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void listComments_withAnonymous_shouldPassNullUid() throws Exception {
        CommentVO vo = new CommentVO();
        vo.setId(1L);
        List<CommentVO> comments = Collections.singletonList(vo);
        when(commentService.listComments(eq(100L), eq(1), eq(10), eq(null)))
                .thenReturn(comments);

        mockMvc.perform(get("/videos/100/comments")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(commentService, times(1))
                .listComments(eq(100L), eq(1), eq(10), eq(null));
    }

    @Test
    public void listComments_withLogin_shouldPassCurrentUid() throws Exception {
        when(commentService.listComments(eq(100L), eq(2), eq(20), eq(1001L)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/videos/100/comments")
                        .param("pageNo", "2")
                        .param("pageSize", "20")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(commentService, times(1))
                .listComments(eq(100L), eq(2), eq(20), eq(1001L));
    }
}
