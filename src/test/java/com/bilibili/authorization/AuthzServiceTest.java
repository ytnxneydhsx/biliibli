package com.bilibili.authorization;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.mapper.CommentMapper;
import com.bilibili.mapper.VideoUploadTaskMapper;
import com.bilibili.model.entity.CommentDO;
import com.bilibili.model.entity.VideoUploadTaskDO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthzServiceTest {

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private VideoUploadTaskMapper videoUploadTaskMapper;

    private AuthzService authzService;

    @Before
    public void setUp() {
        authzService = new AuthzService(commentMapper, videoUploadTaskMapper);
    }

    @Test
    public void canDeleteComment_shouldReturnTrue_whenCurrentUserOwnsComment() {
        CommentDO comment = new CommentDO();
        comment.setId(10L);
        comment.setUserId(1001L);
        comment.setStatus(0);
        when(commentMapper.selectById(10L)).thenReturn(comment);

        boolean allowed = authzService.canDeleteComment(auth(1001L), 10L);

        assertTrue(allowed);
    }

    @Test
    public void canDeleteComment_shouldReturnFalse_whenCurrentUserIsNotOwner() {
        CommentDO comment = new CommentDO();
        comment.setId(10L);
        comment.setUserId(2002L);
        comment.setStatus(0);
        when(commentMapper.selectById(10L)).thenReturn(comment);

        boolean allowed = authzService.canDeleteComment(auth(1001L), 10L);

        assertFalse(allowed);
    }

    @Test
    public void canAccessUploadTask_shouldReturnTrue_whenCurrentUserOwnsTask() {
        VideoUploadTaskDO task = new VideoUploadTaskDO();
        task.setUploadId("u123");
        task.setUserId(1001L);
        when(videoUploadTaskMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(task);

        boolean allowed = authzService.canAccessUploadTask(auth(1001L), "u123");

        assertTrue(allowed);
    }

    @Test
    public void canAccessUploadTask_shouldReturnFalse_whenPrincipalIsAnonymous() {
        boolean allowed = authzService.canAccessUploadTask(
                new UsernamePasswordAuthenticationToken("anonymousUser", null),
                "u123"
        );

        assertFalse(allowed);
    }

    private Authentication auth(Long uid) {
        return new UsernamePasswordAuthenticationToken(new AuthenticatedUser(uid), null);
    }
}
