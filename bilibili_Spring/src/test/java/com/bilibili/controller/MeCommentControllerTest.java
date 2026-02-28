package com.bilibili.controller;

import com.bilibili.common.exception.GlobalExceptionHandler;
import com.bilibili.controller.support.TestAuthenticatedUserArgumentResolver;
import com.bilibili.model.dto.CommentCreateDTO;
import com.bilibili.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class MeCommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private MeCommentController meCommentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(meCommentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new TestAuthenticatedUserArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void createComment_shouldUseCurrentUid() throws Exception {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setContent("hello");
        when(commentService.createComment(eq(1001L), eq(200L), any(CommentCreateDTO.class))).thenReturn(300L);

        mockMvc.perform(post("/me/videos/200/comments")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(300L));

        verify(commentService, times(1)).createComment(eq(1001L), eq(200L), any(CommentCreateDTO.class));
    }

    @Test
    public void deleteComment_shouldUseCurrentUid() throws Exception {
        mockMvc.perform(delete("/me/comments/300")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(commentService, times(1)).deleteComment(eq(1001L), eq(300L));
    }

    @Test
    public void likeComment_shouldUseCurrentUid() throws Exception {
        mockMvc.perform(post("/me/comments/300/likes")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(commentService, times(1)).likeComment(eq(1001L), eq(300L));
    }

    @Test
    public void unlikeComment_shouldUseCurrentUid() throws Exception {
        mockMvc.perform(delete("/me/comments/300/likes")
                        .header(TestAuthenticatedUserArgumentResolver.UID_HEADER, "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(commentService, times(1)).unlikeComment(eq(1001L), eq(300L));
    }
}
