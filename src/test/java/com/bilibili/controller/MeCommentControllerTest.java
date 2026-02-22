package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.model.dto.CommentCreateDTO;
import com.bilibili.service.CommentService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MeCommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private MeCommentController meCommentController;

    @Test
    public void createComment_shouldUseCurrentUid() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setContent("hello");
        when(commentService.createComment(eq(1001L), eq(200L), eq(dto))).thenReturn(300L);

        Long commentId = meCommentController
                .createComment(new AuthenticatedUser(1001L), 200L, dto)
                .getData();

        Assert.assertEquals(Long.valueOf(300L), commentId);
        verify(commentService, times(1)).createComment(eq(1001L), eq(200L), eq(dto));
    }

    @Test
    public void deleteComment_shouldUseCurrentUid() {
        meCommentController.deleteComment(new AuthenticatedUser(1001L), 300L);
        verify(commentService, times(1)).deleteComment(eq(1001L), eq(300L));
    }

    @Test
    public void likeComment_shouldUseCurrentUid() {
        meCommentController.likeComment(new AuthenticatedUser(1001L), 300L);
        verify(commentService, times(1)).likeComment(eq(1001L), eq(300L));
    }

    @Test
    public void unlikeComment_shouldUseCurrentUid() {
        meCommentController.unlikeComment(new AuthenticatedUser(1001L), 300L);
        verify(commentService, times(1)).unlikeComment(eq(1001L), eq(300L));
    }
}
