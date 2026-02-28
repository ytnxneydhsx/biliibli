package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilibili.mapper.CommentMapper;
import com.bilibili.mapper.DanmakuMapper;
import com.bilibili.mapper.FollowingMapper;
import com.bilibili.mapper.TagMapper;
import com.bilibili.mapper.UserInfoMapper;
import com.bilibili.mapper.VideoLikeMapper;
import com.bilibili.mapper.VideoMapper;
import com.bilibili.mapper.VideoTagMapper;
import com.bilibili.model.entity.TagDO;
import com.bilibili.model.entity.UserInfoDO;
import com.bilibili.model.entity.VideoDO;
import com.bilibili.model.entity.VideoLikeDO;
import com.bilibili.model.entity.VideoTagDO;
import com.bilibili.model.vo.VideoDetailVO;
import com.bilibili.model.vo.VideoVO;
import org.junit.Assert;
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
public class VideoServiceImplTest {

    @Mock
    private VideoMapper videoMapper;
    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private VideoTagMapper videoTagMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private VideoLikeMapper videoLikeMapper;
    @Mock
    private FollowingMapper followingMapper;
    @Mock
    private DanmakuMapper danmakuMapper;
    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private VideoServiceImpl videoService;

    @Test
    public void listHomepageVideos_shouldUseDefaultPageWhenParamsInvalid() {
        VideoVO item = new VideoVO();
        item.setId(21L);
        when(videoMapper.selectPublishedVideos(eq(null), eq(0), eq(10)))
                .thenReturn(Collections.singletonList(item));

        List<VideoVO> result = videoService.listHomepageVideos("   ", 0, -1);

        Assert.assertEquals(1, result.size());
        verify(videoMapper, times(1))
                .selectPublishedVideos(eq(null), eq(0), eq(10));
    }

    @Test
    public void listHomepageVideos_shouldClampPageSize() {
        when(videoMapper.selectPublishedVideos(eq("abc"), eq(100), eq(50)))
                .thenReturn(Collections.emptyList());

        List<VideoVO> result = videoService.listHomepageVideos("abc", 3, 999);

        Assert.assertTrue(result.isEmpty());
        verify(videoMapper, times(1))
                .selectPublishedVideos(eq("abc"), eq(100), eq(50));
    }

    @Test
    public void listPublishedVideos_shouldUseDefaultPageWhenParamsInvalid() {
        VideoVO item = new VideoVO();
        item.setId(11L);
        when(videoMapper.selectMyPublishedVideos(eq(1001L), eq(null), eq(0), eq(10)))
                .thenReturn(Collections.singletonList(item));

        List<VideoVO> result = videoService.listPublishedVideos(1001L, "   ", 0, -1);

        Assert.assertEquals(1, result.size());
        verify(videoMapper, times(1))
                .selectMyPublishedVideos(eq(1001L), eq(null), eq(0), eq(10));
    }

    @Test
    public void listPublishedVideos_uidInvalid_shouldThrow() {
        IllegalArgumentException ex = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> videoService.listPublishedVideos(0L, null, 1, 10)
        );

        Assert.assertTrue(ex.getMessage().contains("uid is invalid"));
    }

    @Test
    public void getVideoDetail_shouldAggregateAllFields() {
        VideoDO video = new VideoDO();
        video.setId(200L);
        video.setUserId(1001L);
        video.setTitle("demo");
        video.setDescription("desc");
        video.setVideoUrl("http://x/video.mp4");
        video.setCoverUrl("http://x/cover.jpg");
        video.setDuration(123L);
        video.setViewCount(1000L);
        video.setLikeCount(99L);
        video.setStatus(0);
        when(videoMapper.selectById(200L)).thenReturn(video);

        UserInfoDO userInfo = new UserInfoDO();
        userInfo.setUserId(1001L);
        userInfo.setNickname("tom");
        userInfo.setAvatarUrl("http://x/a.png");
        userInfo.setSign("hi");
        when(userInfoMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userInfo);

        VideoTagDO t1 = new VideoTagDO();
        t1.setTagId(10L);
        VideoTagDO t2 = new VideoTagDO();
        t2.setTagId(11L);
        when(videoTagMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(t1, t2));

        TagDO tag1 = new TagDO();
        tag1.setId(10L);
        tag1.setName("java");
        tag1.setStatus(0);
        TagDO tag2 = new TagDO();
        tag2.setId(11L);
        tag2.setName("spring");
        tag2.setStatus(0);
        when(tagMapper.selectBatchIds(any())).thenReturn(Arrays.asList(tag1, tag2));

        when(danmakuMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
        when(commentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
        when(videoLikeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(followingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        VideoDetailVO vo = videoService.getVideoDetail(200L, 3001L);

        Assert.assertEquals(Long.valueOf(200L), vo.getId());
        Assert.assertEquals("demo", vo.getTitle());
        Assert.assertEquals("desc", vo.getDesc());
        Assert.assertEquals(Long.valueOf(1000L), vo.getViewCount());
        Assert.assertEquals(Long.valueOf(99L), vo.getLikeCount());
        Assert.assertEquals(Long.valueOf(5L), vo.getDanmakuCount());
        Assert.assertEquals(Long.valueOf(3L), vo.getCommentCount());
        Assert.assertTrue(vo.getIsLiked());
        Assert.assertTrue(vo.getIsFollowed());
        Assert.assertEquals(Long.valueOf(1001L), vo.getAuthor().getUid());
        Assert.assertEquals("tom", vo.getAuthor().getNickname());
        Assert.assertEquals(2, vo.getTags().size());
    }

    @Test
    public void getVideoDetail_withoutLogin_shouldSkipInteractionQuery() {
        VideoDO video = new VideoDO();
        video.setId(201L);
        video.setUserId(1001L);
        video.setStatus(0);
        when(videoMapper.selectById(201L)).thenReturn(video);
        when(videoTagMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(danmakuMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(commentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        VideoDetailVO vo = videoService.getVideoDetail(201L, null);

        Assert.assertFalse(vo.getIsLiked());
        Assert.assertFalse(vo.getIsFollowed());
        verify(videoLikeMapper, never()).selectCount(any(LambdaQueryWrapper.class));
        verify(followingMapper, never()).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    public void validateViewableVideo_shouldCheckExists() {
        when(videoMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        videoService.validateViewableVideo(300L);

        verify(videoMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    public void likeVideo_newRelation_shouldInsertAndIncreaseCount() {
        when(videoMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(videoLikeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(videoLikeMapper.insert(any(VideoLikeDO.class))).thenReturn(1);
        when(videoMapper.update(isNull(), any())).thenReturn(1);

        videoService.likeVideo(1001L, 300L);

        verify(videoLikeMapper, times(1)).insert(any(VideoLikeDO.class));
        verify(videoMapper, times(1)).update(isNull(), any());
    }

    @Test
    public void likeVideo_alreadyLiked_shouldIdempotent() {
        VideoLikeDO relation = new VideoLikeDO();
        relation.setId(1L);
        relation.setStatus(0);
        when(videoMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(videoLikeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(relation);

        videoService.likeVideo(1001L, 300L);

        verify(videoLikeMapper, never()).insert(any(VideoLikeDO.class));
    }

    @Test
    public void unlikeVideo_shouldCancelAndDecreaseCount() {
        when(videoLikeMapper.update(isNull(), any())).thenReturn(1);
        when(videoMapper.update(isNull(), any())).thenReturn(1);

        videoService.unlikeVideo(1001L, 300L);

        verify(videoLikeMapper, times(1)).update(isNull(), any());
        verify(videoMapper, times(1)).update(isNull(), any());
    }
}
