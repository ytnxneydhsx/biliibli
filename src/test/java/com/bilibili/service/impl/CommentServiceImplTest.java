package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.bilibili.common.exception.ForbiddenException;
import com.bilibili.mapper.CommentLikeMapper;
import com.bilibili.mapper.CommentMapper;
import com.bilibili.mapper.UserInfoMapper;
import com.bilibili.mapper.VideoMapper;
import com.bilibili.model.dto.CommentCreateDTO;
import com.bilibili.model.entity.CommentDO;
import com.bilibili.model.entity.CommentLikeDO;
import com.bilibili.model.entity.UserInfoDO;
import com.bilibili.model.entity.VideoDO;
import com.bilibili.model.vo.CommentVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommentServiceImplTest {

    @BeforeClass
    public static void initMybatisPlusLambdaCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, CommentDO.class);
        TableInfoHelper.initTableInfo(assistant, CommentLikeDO.class);
        TableInfoHelper.initTableInfo(assistant, UserInfoDO.class);
        TableInfoHelper.initTableInfo(assistant, VideoDO.class);
    }

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private CommentLikeMapper commentLikeMapper;
    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private VideoMapper videoMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    public void createRootComment_shouldInsertAndIncreaseVideoCommentCount() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setContent("hello");
        dto.setParentId(0L);

        when(videoMapper.selectCount(any())).thenReturn(1L);
        when(commentMapper.insert(any(CommentDO.class))).thenAnswer(invocation -> {
            CommentDO comment = invocation.getArgument(0);
            comment.setId(500L);
            return 1;
        });
        when(videoMapper.update(isNull(), any())).thenReturn(1);

        Long commentId = commentService.createComment(1001L, 200L, dto);

        Assert.assertEquals(Long.valueOf(500L), commentId);
        verify(commentMapper, times(1)).insert(any(CommentDO.class));
        verify(videoMapper, times(1)).update(isNull(), any());
        verify(commentMapper, never()).update(isNull(), any());
    }

    @Test
    public void createReply_shouldIncreaseParentReplyCount() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setContent("reply");
        dto.setParentId(10L);

        CommentDO parent = new CommentDO();
        parent.setId(10L);
        parent.setVideoId(200L);
        parent.setParentId(0L);
        parent.setStatus(0);

        when(videoMapper.selectCount(any())).thenReturn(1L);
        when(commentMapper.selectById(eq(10L))).thenReturn(parent);
        when(commentMapper.insert(any(CommentDO.class))).thenAnswer(invocation -> {
            CommentDO comment = invocation.getArgument(0);
            comment.setId(501L);
            return 1;
        });
        when(videoMapper.update(isNull(), any())).thenReturn(1);
        when(commentMapper.update(isNull(), any())).thenReturn(1);

        Long commentId = commentService.createComment(1001L, 200L, dto);

        Assert.assertEquals(Long.valueOf(501L), commentId);
        verify(commentMapper, times(1)).insert(any(CommentDO.class));
        verify(commentMapper, times(1)).update(isNull(), any());
        verify(videoMapper, times(1)).update(isNull(), any());
    }

    @Test
    public void createReplyToReply_shouldThrow() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setContent("reply");
        dto.setParentId(11L);

        CommentDO parent = new CommentDO();
        parent.setId(11L);
        parent.setVideoId(200L);
        parent.setParentId(10L);
        parent.setStatus(0);

        when(videoMapper.selectCount(any())).thenReturn(1L);
        when(commentMapper.selectById(eq(11L))).thenReturn(parent);

        IllegalArgumentException ex = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> commentService.createComment(1001L, 200L, dto)
        );
        Assert.assertTrue(ex.getMessage().contains("one-level reply"));
    }

    @Test
    public void listComments_shouldAssembleRootsRepliesAndLikeFlag() {
        when(videoMapper.selectCount(any())).thenReturn(1L);

        CommentDO root = new CommentDO();
        root.setId(1L);
        root.setVideoId(200L);
        root.setUserId(1001L);
        root.setParentId(0L);
        root.setRootId(0L);
        root.setStatus(0);
        root.setContent("root");
        root.setReplyCount(1);
        root.setLikeCount(2L);

        CommentDO reply = new CommentDO();
        reply.setId(2L);
        reply.setVideoId(200L);
        reply.setUserId(1002L);
        reply.setParentId(1L);
        reply.setRootId(1L);
        reply.setStatus(0);
        reply.setContent("reply");
        reply.setReplyCount(0);
        reply.setLikeCount(0L);

        when(commentMapper.selectList(any())).thenReturn(
                Collections.singletonList(root),
                Collections.singletonList(reply)
        );

        UserInfoDO user1 = new UserInfoDO();
        user1.setUserId(1001L);
        user1.setNickname("u1");
        user1.setAvatarUrl("a1");
        UserInfoDO user2 = new UserInfoDO();
        user2.setUserId(1002L);
        user2.setNickname("u2");
        user2.setAvatarUrl("a2");
        when(userInfoMapper.selectList(any())).thenReturn(Arrays.asList(user1, user2));

        CommentLikeDO like = new CommentLikeDO();
        like.setCommentId(2L);
        like.setUserId(1001L);
        like.setStatus(0);
        when(commentLikeMapper.selectList(any())).thenReturn(Collections.singletonList(like));

        List<CommentVO> list = commentService.listComments(200L, 1, 10, 1001L);

        Assert.assertEquals(1, list.size());
        CommentVO rootVo = list.get(0);
        Assert.assertEquals(Long.valueOf(1L), rootVo.getId());
        Assert.assertEquals(1, rootVo.getChildComments().size());
        Assert.assertFalse(rootVo.getIsLiked());
        Assert.assertTrue(rootVo.getChildComments().get(0).getIsLiked());
    }

    @Test
    public void deleteRootComment_shouldCascadeReplies() {
        CommentDO root = new CommentDO();
        root.setId(10L);
        root.setUserId(1001L);
        root.setVideoId(200L);
        root.setParentId(0L);
        root.setStatus(0);

        when(commentMapper.selectById(eq(10L))).thenReturn(root);
        when(commentMapper.update(isNull(), any())).thenReturn(1, 2);
        when(videoMapper.update(isNull(), any())).thenReturn(1);

        commentService.deleteComment(1001L, 10L);

        verify(commentMapper, times(2)).update(isNull(), any());
        verify(videoMapper, times(1)).update(isNull(), any());
    }

    @Test
    public void deleteCommentByOtherUser_shouldThrowForbidden() {
        CommentDO comment = new CommentDO();
        comment.setId(10L);
        comment.setUserId(2002L);
        comment.setVideoId(200L);
        comment.setParentId(0L);
        comment.setStatus(0);
        when(commentMapper.selectById(eq(10L))).thenReturn(comment);

        Assert.assertThrows(ForbiddenException.class, () -> commentService.deleteComment(1001L, 10L));
    }

    @Test
    public void likeComment_newRelation_shouldInsertAndIncreaseCount() {
        when(commentMapper.selectCount(any())).thenReturn(1L);
        when(commentLikeMapper.selectOne(any())).thenReturn(null);
        when(commentLikeMapper.insert(any(CommentLikeDO.class))).thenReturn(1);
        when(commentMapper.update(isNull(), any())).thenReturn(1);

        commentService.likeComment(1001L, 10L);

        verify(commentLikeMapper, times(1)).insert(any(CommentLikeDO.class));
        verify(commentMapper, times(1)).update(isNull(), any());
    }

    @Test
    public void likeComment_alreadyLiked_shouldIdempotent() {
        CommentLikeDO relation = new CommentLikeDO();
        relation.setId(1L);
        relation.setStatus(0);
        when(commentMapper.selectCount(any())).thenReturn(1L);
        when(commentLikeMapper.selectOne(any())).thenReturn(relation);

        commentService.likeComment(1001L, 10L);

        verify(commentLikeMapper, never()).insert(any(CommentLikeDO.class));
    }

    @Test
    public void unlikeComment_shouldCancelAndDecreaseCount() {
        when(commentMapper.selectCount(any())).thenReturn(1L);
        when(commentLikeMapper.update(isNull(), any())).thenReturn(1);
        when(commentMapper.update(isNull(), any())).thenReturn(1);

        commentService.unlikeComment(1001L, 10L);

        verify(commentLikeMapper, times(1)).update(isNull(), any());
        verify(commentMapper, times(1)).update(isNull(), any());
    }
}
