package com.bilibili.authorization;

import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.comment.mapper.CommentMapper;
import com.bilibili.comment.model.entity.CommentDO;
import com.bilibili.upload.video.mapper.VideoUploadTaskMapper;
import com.bilibili.upload.video.model.entity.VideoUploadTaskDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthzServiceTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private VideoUploadTaskMapper videoUploadTaskMapper;

    @InjectMocks
    private AuthzService authzService;

    @Test
    void canDeleteComment_shouldReturnTrueForOwnerAndNormalStatus() {
        CommentDO comment = new CommentDO();
        comment.setId(200L);
        comment.setUserId(1001L);
        comment.setStatus(0);
        when(commentMapper.selectById(200L)).thenReturn(comment);

        boolean allowed = authzService.canDeleteComment(auth(1001L), 200L);

        assertTrue(allowed);
    }

    @Test
    void canDeleteComment_shouldReturnFalseForNonOwner() {
        CommentDO comment = new CommentDO();
        comment.setId(200L);
        comment.setUserId(1002L);
        comment.setStatus(0);
        when(commentMapper.selectById(200L)).thenReturn(comment);

        boolean allowed = authzService.canDeleteComment(auth(1001L), 200L);

        assertFalse(allowed);
    }

    @Test
    void canAccessUploadTask_shouldReturnFalseForBlankUploadId() {
        assertFalse(authzService.canAccessUploadTask(auth(1001L), "   "));
    }

    @Test
    void canAccessUploadTask_shouldReturnTrueForOwner() {
        VideoUploadTaskDO task = new VideoUploadTaskDO();
        task.setUserId(1001L);
        when(videoUploadTaskMapper.selectOne(any())).thenReturn(task);

        boolean allowed = authzService.canAccessUploadTask(auth(1001L), " upload-1 ");

        assertTrue(allowed);
    }

    private static Authentication auth(Long uid) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(uid),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
