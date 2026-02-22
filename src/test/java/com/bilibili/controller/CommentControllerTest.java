package com.bilibili.controller;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.model.vo.CommentVO;
import com.bilibili.service.CommentService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    @Test
    public void listComments_withAnonymous_shouldPassNullUid() {
        CommentVO vo = new CommentVO();
        vo.setId(1L);
        List<CommentVO> comments = Collections.singletonList(vo);
        when(commentService.listComments(eq(100L), eq(1), eq(10), eq(null)))
                .thenReturn(comments);

        List<CommentVO> result = commentController.listComments(100L, 1, 10, null).getData();

        Assert.assertEquals(1, result.size());
        verify(commentService, times(1))
                .listComments(eq(100L), eq(1), eq(10), eq(null));
    }

    @Test
    public void listComments_withLogin_shouldPassCurrentUid() {
        when(commentService.listComments(eq(100L), eq(2), eq(20), eq(1001L)))
                .thenReturn(Collections.emptyList());

        List<CommentVO> result = commentController
                .listComments(100L, 2, 20, new AuthenticatedUser(1001L))
                .getData();

        Assert.assertTrue(result.isEmpty());
        verify(commentService, times(1))
                .listComments(eq(100L), eq(2), eq(20), eq(1001L));
    }
}

