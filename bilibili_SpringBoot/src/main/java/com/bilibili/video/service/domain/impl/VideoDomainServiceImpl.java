package com.bilibili.video.service.domain.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.comment.mapper.CommentMapper;
import com.bilibili.comment.model.entity.CommentDO;
import com.bilibili.common.enums.RecordStatus;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.following.mapper.FollowingMapper;
import com.bilibili.following.model.entity.FollowingDO;
import com.bilibili.user.mapper.UserInfoMapper;
import com.bilibili.user.model.entity.UserInfoDO;
import com.bilibili.video.mapper.DanmakuMapper;
import com.bilibili.video.mapper.TagMapper;
import com.bilibili.video.mapper.VideoLikeMapper;
import com.bilibili.video.mapper.VideoMapper;
import com.bilibili.video.mapper.VideoTagMapper;
import com.bilibili.video.model.entity.DanmakuDO;
import com.bilibili.video.model.entity.TagDO;
import com.bilibili.video.model.entity.VideoDO;
import com.bilibili.video.model.entity.VideoLikeDO;
import com.bilibili.video.model.entity.VideoTagDO;
import com.bilibili.video.model.vo.VideoDetailVO;
import com.bilibili.video.model.vo.VideoVO;
import com.bilibili.video.service.domain.VideoDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class VideoDomainServiceImpl implements VideoDomainService {

    private final VideoMapper videoMapper;
    private final UserInfoMapper userInfoMapper;
    private final VideoTagMapper videoTagMapper;
    private final TagMapper tagMapper;
    private final VideoLikeMapper videoLikeMapper;
    private final FollowingMapper followingMapper;
    private final DanmakuMapper danmakuMapper;
    private final CommentMapper commentMapper;

    public VideoDomainServiceImpl(VideoMapper videoMapper,
                                  UserInfoMapper userInfoMapper,
                                  VideoTagMapper videoTagMapper,
                                  TagMapper tagMapper,
                                  VideoLikeMapper videoLikeMapper,
                                  FollowingMapper followingMapper,
                                  DanmakuMapper danmakuMapper,
                                  CommentMapper commentMapper) {
        this.videoMapper = videoMapper;
        this.userInfoMapper = userInfoMapper;
        this.videoTagMapper = videoTagMapper;
        this.tagMapper = tagMapper;
        this.videoLikeMapper = videoLikeMapper;
        this.followingMapper = followingMapper;
        this.danmakuMapper = danmakuMapper;
        this.commentMapper = commentMapper;
    }

    @Override
    public IPage<VideoVO> listHomepageVideos(String title, PageQueryDTO pageQuery) {
        PageQueryDTO query = pageQuery == null ? new PageQueryDTO() : pageQuery;
        int normalizedPageNo = query.normalizedPageNo();
        int normalizedPageSize = query.normalizedPageSize();
        String normalizedTitle = normalizeOptional(title);

        Page<VideoVO> page = new Page<>(normalizedPageNo, normalizedPageSize);
        return videoMapper.selectPublishedVideos(page, normalizedTitle);
    }

    @Override
    public IPage<VideoVO> listPublishedVideos(Long uid, String title, PageQueryDTO pageQuery) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }

        PageQueryDTO query = pageQuery == null ? new PageQueryDTO() : pageQuery;
        int normalizedPageNo = query.normalizedPageNo();
        int normalizedPageSize = query.normalizedPageSize();
        String normalizedTitle = normalizeOptional(title);

        Page<VideoVO> page = new Page<>(normalizedPageNo, normalizedPageSize);
        return videoMapper.selectMyPublishedVideos(page, uid, normalizedTitle);
    }

    @Override
    public VideoDetailVO getVideoDetail(Long videoId, Long currentUid) {
        if (videoId == null || videoId <= 0) {
            throw new IllegalArgumentException("videoId is invalid");
        }

        VideoDO video = videoMapper.selectById(videoId);
        if (video == null || !RecordStatus.NORMAL.matches(video.getStatus())) {
            throw new IllegalArgumentException("video not found");
        }

        VideoDetailVO vo = new VideoDetailVO();
        vo.setId(video.getId());
        vo.setVideoUrl(video.getVideoUrl());
        vo.setTitle(video.getTitle());
        vo.setDesc(video.getDescription());
        vo.setCoverUrl(video.getCoverUrl());
        vo.setDuration(video.getDuration());
        vo.setUploadDate(video.getCreateTime());
        vo.setViewCount(video.getViewCount());
        vo.setLikeCount(video.getLikeCount());

        vo.setAuthor(buildAuthor(video.getUserId()));
        vo.setTags(queryTagNames(video.getId()));
        vo.setDanmakuCount(countDanmaku(video.getId()));
        Long commentCount = video.getCommentCount();
        vo.setCommentCount(commentCount == null ? countComment(video.getId()) : commentCount);

        boolean hasLogin = currentUid != null && currentUid > 0;
        vo.setIsLiked(hasLogin && isVideoLikedByCurrentUser(video.getId(), currentUid));
        vo.setIsFollowed(hasLogin && isFollowedByCurrentUser(video.getUserId(), currentUid));
        return vo;
    }

    @Override
    public void validateViewableVideo(Long videoId) {
        if (videoId == null || videoId <= 0) {
            throw new IllegalArgumentException("videoId is invalid");
        }
        ensureVideoExists(videoId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeVideo(Long uid, Long videoId) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        if (videoId == null || videoId <= 0) {
            throw new IllegalArgumentException("videoId is invalid");
        }

        ensureVideoExists(videoId);

        LambdaQueryWrapper<VideoLikeDO> query = new LambdaQueryWrapper<>();
        query.eq(VideoLikeDO::getVideoId, videoId)
                .eq(VideoLikeDO::getUserId, uid);
        VideoLikeDO relation = videoLikeMapper.selectOne(query);

        if (relation == null) {
            VideoLikeDO newRelation = new VideoLikeDO();
            newRelation.setVideoId(videoId);
            newRelation.setUserId(uid);
            newRelation.setStatus(RecordStatus.NORMAL.code());
            int insertRows = videoLikeMapper.insert(newRelation);
            if (insertRows != 1) {
                throw new RuntimeException("insert like relation failed");
            }
            increaseVideoLikeCount(videoId);
            return;
        }

        if (RecordStatus.NORMAL.matches(relation.getStatus())) {
            return;
        }

        UpdateWrapper<VideoLikeDO> reactivate = new UpdateWrapper<>();
        reactivate.eq("id", relation.getId())
                .eq("status", RecordStatus.DELETED.code())
                .set("status", RecordStatus.NORMAL.code());
        int reactivateRows = videoLikeMapper.update(null, reactivate);
        if (reactivateRows != 1) {
            return;
        }
        increaseVideoLikeCount(videoId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeVideo(Long uid, Long videoId) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        if (videoId == null || videoId <= 0) {
            throw new IllegalArgumentException("videoId is invalid");
        }

        UpdateWrapper<VideoLikeDO> cancel = new UpdateWrapper<>();
        cancel.eq("video_id", videoId)
                .eq("user_id", uid)
                .eq("status", RecordStatus.NORMAL.code())
                .set("status", RecordStatus.DELETED.code());
        int rows = videoLikeMapper.update(null, cancel);

        if (rows == 0) {
            return;
        }
        if (rows != 1) {
            throw new RuntimeException("cancel like relation failed");
        }

        decreaseVideoLikeCount(videoId);
    }

    private VideoDetailVO.AuthorVO buildAuthor(Long authorUid) {
        VideoDetailVO.AuthorVO author = new VideoDetailVO.AuthorVO();
        author.setUid(authorUid);

        LambdaQueryWrapper<UserInfoDO> query = new LambdaQueryWrapper<>();
        query.eq(UserInfoDO::getUserId, authorUid);
        UserInfoDO userInfo = userInfoMapper.selectOne(query);
        if (userInfo == null) {
            return author;
        }

        author.setNickname(userInfo.getNickname());
        author.setAvatar(userInfo.getAvatarUrl());
        author.setSign(userInfo.getSign());
        return author;
    }

    private List<String> queryTagNames(Long videoId) {
        LambdaQueryWrapper<VideoTagDO> videoTagQuery = new LambdaQueryWrapper<>();
        videoTagQuery.eq(VideoTagDO::getVideoId, videoId)
                .eq(VideoTagDO::getStatus, RecordStatus.NORMAL.code());
        List<VideoTagDO> videoTags = videoTagMapper.selectList(videoTagQuery);
        if (videoTags == null || videoTags.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> tagIds = videoTags.stream()
                .map(VideoTagDO::getTagId)
                .distinct()
                .collect(Collectors.toList());

        List<TagDO> tags = tagMapper.selectBatchIds(tagIds);
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }

        return tags.stream()
                .filter(tag -> RecordStatus.NORMAL.matches(tag.getStatus()))
                .map(TagDO::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Long countDanmaku(Long videoId) {
        LambdaQueryWrapper<DanmakuDO> query = new LambdaQueryWrapper<>();
        query.eq(DanmakuDO::getVideoId, videoId)
                .eq(DanmakuDO::getStatus, RecordStatus.NORMAL.code());
        Long count = danmakuMapper.selectCount(query);
        return count == null ? 0L : count;
    }

    private Long countComment(Long videoId) {
        LambdaQueryWrapper<CommentDO> query = new LambdaQueryWrapper<>();
        query.eq(CommentDO::getVideoId, videoId)
                .eq(CommentDO::getStatus, RecordStatus.NORMAL.code());
        Long count = commentMapper.selectCount(query);
        return count == null ? 0L : count;
    }

    private boolean isVideoLikedByCurrentUser(Long videoId, Long currentUid) {
        LambdaQueryWrapper<VideoLikeDO> query = new LambdaQueryWrapper<>();
        query.eq(VideoLikeDO::getVideoId, videoId)
                .eq(VideoLikeDO::getUserId, currentUid)
                .eq(VideoLikeDO::getStatus, RecordStatus.NORMAL.code());
        Long count = videoLikeMapper.selectCount(query);
        return count != null && count > 0;
    }

    private boolean isFollowedByCurrentUser(Long authorUid, Long currentUid) {
        LambdaQueryWrapper<FollowingDO> query = new LambdaQueryWrapper<>();
        query.eq(FollowingDO::getUserId, currentUid)
                .eq(FollowingDO::getFollowingUserId, authorUid)
                .eq(FollowingDO::getStatus, RecordStatus.NORMAL.code());
        Long count = followingMapper.selectCount(query);
        return count != null && count > 0;
    }

    private void ensureVideoExists(Long videoId) {
        LambdaQueryWrapper<VideoDO> query = new LambdaQueryWrapper<>();
        query.eq(VideoDO::getId, videoId)
                .eq(VideoDO::getStatus, RecordStatus.NORMAL.code());
        Long count = videoMapper.selectCount(query);
        if (count == null || count <= 0) {
            throw new IllegalArgumentException("video not found");
        }
    }

    private void increaseVideoLikeCount(Long videoId) {
        LambdaUpdateWrapper<VideoDO> update = new LambdaUpdateWrapper<>();
        update.eq(VideoDO::getId, videoId)
                .eq(VideoDO::getStatus, RecordStatus.NORMAL.code())
                .setSql("like_count = like_count + 1");
        int rows = videoMapper.update(null, update);
        if (rows != 1) {
            throw new RuntimeException("increase like_count failed");
        }
    }

    private void decreaseVideoLikeCount(Long videoId) {
        LambdaUpdateWrapper<VideoDO> update = new LambdaUpdateWrapper<>();
        update.eq(VideoDO::getId, videoId)
                .eq(VideoDO::getStatus, RecordStatus.NORMAL.code())
                .setSql("like_count = GREATEST(like_count - 1, 0)");
        int rows = videoMapper.update(null, update);
        if (rows != 1) {
            throw new RuntimeException("decrease like_count failed");
        }
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
