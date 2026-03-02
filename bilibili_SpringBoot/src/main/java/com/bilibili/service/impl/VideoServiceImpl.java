package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.mapper.CommentMapper;
import com.bilibili.mapper.DanmakuMapper;
import com.bilibili.mapper.FollowingMapper;
import com.bilibili.mapper.TagMapper;
import com.bilibili.mapper.UserInfoMapper;
import com.bilibili.mapper.VideoMapper;
import com.bilibili.mapper.VideoLikeMapper;
import com.bilibili.mapper.VideoTagMapper;
import com.bilibili.model.entity.CommentDO;
import com.bilibili.model.entity.DanmakuDO;
import com.bilibili.model.entity.FollowingDO;
import com.bilibili.model.entity.TagDO;
import com.bilibili.model.entity.UserInfoDO;
import com.bilibili.model.entity.VideoDO;
import com.bilibili.model.entity.VideoLikeDO;
import com.bilibili.model.entity.VideoTagDO;
import com.bilibili.model.vo.VideoDetailVO;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class VideoServiceImpl implements VideoService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int STATUS_NORMAL = 0;

    private final VideoMapper videoMapper;
    private final UserInfoMapper userInfoMapper;
    private final VideoTagMapper videoTagMapper;
    private final TagMapper tagMapper;
    private final VideoLikeMapper videoLikeMapper;
    private final FollowingMapper followingMapper;
    private final DanmakuMapper danmakuMapper;
    private final CommentMapper commentMapper;

    @Autowired
    public VideoServiceImpl(VideoMapper videoMapper,
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
    public IPage<VideoVO> listHomepageVideos(String title, Integer pageNo, Integer pageSize) {
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        String normalizedTitle = normalizeOptional(title);

        Page<VideoVO> page = new Page<>(normalizedPageNo, normalizedPageSize);
        return videoMapper.selectPublishedVideos(page, normalizedTitle);
    }

    @Override
    public IPage<VideoVO> listPublishedVideos(Long uid, String title, Integer pageNo, Integer pageSize) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }

        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
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
        if (video == null || !Objects.equals(video.getStatus(), STATUS_NORMAL)) {
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
            newRelation.setStatus(STATUS_NORMAL);
            int insertRows = videoLikeMapper.insert(newRelation);
            if (insertRows != 1) {
                throw new RuntimeException("insert like relation failed");
            }
            increaseVideoLikeCount(videoId);
            return;
        }

        if (Objects.equals(relation.getStatus(), STATUS_NORMAL)) {
            return;
        }

        UpdateWrapper<VideoLikeDO> reactivate = new UpdateWrapper<>();
        reactivate.eq("id", relation.getId())
                .eq("status", 1)
                .set("status", STATUS_NORMAL);
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
                .eq("status", STATUS_NORMAL)
                .set("status", 1);
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
                .eq(VideoTagDO::getStatus, STATUS_NORMAL);
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
                .filter(tag -> Objects.equals(tag.getStatus(), STATUS_NORMAL))
                .map(TagDO::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Long countDanmaku(Long videoId) {
        LambdaQueryWrapper<DanmakuDO> query = new LambdaQueryWrapper<>();
        query.eq(DanmakuDO::getVideoId, videoId)
                .eq(DanmakuDO::getStatus, STATUS_NORMAL);
        Long count = danmakuMapper.selectCount(query);
        return count == null ? 0L : count;
    }

    private Long countComment(Long videoId) {
        LambdaQueryWrapper<CommentDO> query = new LambdaQueryWrapper<>();
        query.eq(CommentDO::getVideoId, videoId)
                .eq(CommentDO::getStatus, STATUS_NORMAL);
        Long count = commentMapper.selectCount(query);
        return count == null ? 0L : count;
    }

    private boolean isVideoLikedByCurrentUser(Long videoId, Long currentUid) {
        LambdaQueryWrapper<VideoLikeDO> query = new LambdaQueryWrapper<>();
        query.eq(VideoLikeDO::getVideoId, videoId)
                .eq(VideoLikeDO::getUserId, currentUid)
                .eq(VideoLikeDO::getStatus, STATUS_NORMAL);
        Long count = videoLikeMapper.selectCount(query);
        return count != null && count > 0;
    }

    private boolean isFollowedByCurrentUser(Long authorUid, Long currentUid) {
        LambdaQueryWrapper<FollowingDO> query = new LambdaQueryWrapper<>();
        query.eq(FollowingDO::getUserId, currentUid)
                .eq(FollowingDO::getFollowingUserId, authorUid)
                .eq(FollowingDO::getStatus, STATUS_NORMAL);
        Long count = followingMapper.selectCount(query);
        return count != null && count > 0;
    }

    private void ensureVideoExists(Long videoId) {
        LambdaQueryWrapper<VideoDO> query = new LambdaQueryWrapper<>();
        query.eq(VideoDO::getId, videoId)
                .eq(VideoDO::getStatus, STATUS_NORMAL);
        Long count = videoMapper.selectCount(query);
        if (count == null || count <= 0) {
            throw new IllegalArgumentException("video not found");
        }
    }

    private void increaseVideoLikeCount(Long videoId) {
        LambdaUpdateWrapper<VideoDO> update = new LambdaUpdateWrapper<>();
        update.eq(VideoDO::getId, videoId)
                .eq(VideoDO::getStatus, STATUS_NORMAL)
                .setSql("like_count = like_count + 1");
        int rows = videoMapper.update(null, update);
        if (rows != 1) {
            throw new RuntimeException("increase like_count failed");
        }
    }

    private void decreaseVideoLikeCount(Long videoId) {
        LambdaUpdateWrapper<VideoDO> update = new LambdaUpdateWrapper<>();
        update.eq(VideoDO::getId, videoId)
                .eq(VideoDO::getStatus, STATUS_NORMAL)
                .setSql("like_count = GREATEST(like_count - 1, 0)");
        int rows = videoMapper.update(null, update);
        if (rows != 1) {
            throw new RuntimeException("decrease like_count failed");
        }
    }

    private static int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo <= 0) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    private static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
