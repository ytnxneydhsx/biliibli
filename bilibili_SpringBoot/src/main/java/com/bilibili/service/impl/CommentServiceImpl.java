package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.common.exception.ForbiddenException;
import com.bilibili.common.enums.RecordStatus;
import com.bilibili.mapper.CommentLikeMapper;
import com.bilibili.mapper.CommentMapper;
import com.bilibili.mapper.UserInfoMapper;
import com.bilibili.mapper.VideoMapper;
import com.bilibili.model.dto.CommentCreateDTO;
import com.bilibili.model.dto.PageQueryDTO;
import com.bilibili.model.entity.CommentDO;
import com.bilibili.model.entity.CommentLikeDO;
import com.bilibili.model.entity.UserInfoDO;
import com.bilibili.model.entity.VideoDO;
import com.bilibili.model.vo.CommentVO;
import com.bilibili.service.CommentService;
import com.bilibili.tool.StringTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    private static final int MAX_CONTENT_LENGTH = 1000;

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserInfoMapper userInfoMapper;
    private final VideoMapper videoMapper;

    @Autowired
    public CommentServiceImpl(CommentMapper commentMapper,
                              CommentLikeMapper commentLikeMapper,
                              UserInfoMapper userInfoMapper,
                              VideoMapper videoMapper) {
        this.commentMapper = commentMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.userInfoMapper = userInfoMapper;
        this.videoMapper = videoMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(Long uid, Long videoId, CommentCreateDTO dto) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        if (videoId == null || videoId <= 0) {
            throw new IllegalArgumentException("videoId is invalid");
        }
        if (dto == null) {
            throw new IllegalArgumentException("comment request is null");
        }

        ensureVideoExists(videoId);
        String content = StringTool.normalizeRequired(dto.getContent(), "content");
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("content is too long");
        }

        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        Long rootId = 0L;
        if (parentId < 0) {
            throw new IllegalArgumentException("parentId is invalid");
        }

        if (parentId > 0) {
            CommentDO parent = commentMapper.selectById(parentId);
            if (parent == null || !RecordStatus.NORMAL.matches(parent.getStatus())) {
                throw new IllegalArgumentException("parent comment not found");
            }
            if (!Objects.equals(parent.getVideoId(), videoId)) {
                throw new IllegalArgumentException("parent comment does not belong to this video");
            }
            if (parent.getParentId() != null && parent.getParentId() > 0) {
                throw new IllegalArgumentException("only one-level reply is supported");
            }
            rootId = parent.getId();
        }

        CommentDO comment = new CommentDO();
        comment.setVideoId(videoId);
        comment.setUserId(uid);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setRootId(rootId);
        comment.setLikeCount(0L);
        comment.setReplyCount(0);
        comment.setStatus(RecordStatus.NORMAL.code());
        int insertRows = commentMapper.insert(comment);
        if (insertRows != 1 || comment.getId() == null) {
            throw new RuntimeException("create comment failed");
        }

        increaseVideoCommentCount(videoId, 1);
        if (parentId > 0) {
            increaseParentReplyCount(parentId, 1);
        }
        return comment.getId();
    }

    @Override
    public List<CommentVO> listComments(Long videoId, PageQueryDTO pageQuery, Long currentUid) {
        if (videoId == null || videoId <= 0) {
            throw new IllegalArgumentException("videoId is invalid");
        }
        ensureVideoExists(videoId);

        PageQueryDTO query = pageQuery == null ? new PageQueryDTO() : pageQuery;
        int normalizedPageNo = query.normalizedPageNo();
        int normalizedPageSize = query.normalizedPageSize();

        LambdaQueryWrapper<CommentDO> rootQuery = new LambdaQueryWrapper<>();
        rootQuery.eq(CommentDO::getVideoId, videoId)
                .eq(CommentDO::getStatus, RecordStatus.NORMAL.code())
                .eq(CommentDO::getParentId, 0L)
                .orderByDesc(CommentDO::getCreateTime)
                .orderByDesc(CommentDO::getId);
        Page<CommentDO> page = new Page<>(normalizedPageNo, normalizedPageSize, false);
        List<CommentDO> roots = commentMapper.selectPage(page, rootQuery).getRecords();
        if (roots == null || roots.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> rootIds = roots.stream().map(CommentDO::getId).collect(Collectors.toList());

        LambdaQueryWrapper<CommentDO> replyQuery = new LambdaQueryWrapper<>();
        replyQuery.eq(CommentDO::getVideoId, videoId)
                .eq(CommentDO::getStatus, RecordStatus.NORMAL.code())
                .in(CommentDO::getRootId, rootIds)
                .ne(CommentDO::getParentId, 0L)
                .orderByAsc(CommentDO::getCreateTime)
                .orderByAsc(CommentDO::getId);
        List<CommentDO> replies = commentMapper.selectList(replyQuery);

        List<CommentDO> allComments = new ArrayList<>(roots);
        if (replies != null && !replies.isEmpty()) {
            allComments.addAll(replies);
        }
        Map<Long, UserInfoDO> userInfoMap = queryUserInfos(allComments);
        Set<Long> likedCommentIds = queryLikedCommentIds(allComments, currentUid);

        Map<Long, CommentVO> rootVoMap = new LinkedHashMap<>();
        for (CommentDO root : roots) {
            CommentVO vo = buildCommentVO(root, userInfoMap.get(root.getUserId()), likedCommentIds);
            vo.setChildComments(new ArrayList<>());
            rootVoMap.put(root.getId(), vo);
        }

        if (replies != null) {
            for (CommentDO reply : replies) {
                CommentVO parentRootVo = rootVoMap.get(reply.getRootId());
                if (parentRootVo == null) {
                    continue;
                }
                CommentVO replyVo = buildCommentVO(reply, userInfoMap.get(reply.getUserId()), likedCommentIds);
                parentRootVo.getChildComments().add(replyVo);
            }
        }

        return new ArrayList<>(rootVoMap.values());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long uid, Long commentId) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        if (commentId == null || commentId <= 0) {
            throw new IllegalArgumentException("commentId is invalid");
        }

        CommentDO comment = commentMapper.selectById(commentId);
        if (comment == null || !RecordStatus.NORMAL.matches(comment.getStatus())) {
            throw new IllegalArgumentException("comment not found");
        }
        if (!Objects.equals(comment.getUserId(), uid)) {
            throw new ForbiddenException("cannot delete other user's comment");
        }

        LambdaUpdateWrapper<CommentDO> markDeleted = new LambdaUpdateWrapper<>();
        markDeleted.eq(CommentDO::getId, commentId)
                .eq(CommentDO::getStatus, RecordStatus.NORMAL.code())
                .set(CommentDO::getStatus, RecordStatus.DELETED.code());
        int deletedRows = commentMapper.update(null, markDeleted);
        if (deletedRows != 1) {
            return;
        }

        int totalDeletedCount = 1;
        if (comment.getParentId() != null && comment.getParentId() > 0) {
            increaseParentReplyCount(comment.getParentId(), -1);
        } else {
            LambdaUpdateWrapper<CommentDO> markRepliesDeleted = new LambdaUpdateWrapper<>();
            markRepliesDeleted.eq(CommentDO::getRootId, commentId)
                    .eq(CommentDO::getStatus, RecordStatus.NORMAL.code())
                    .set(CommentDO::getStatus, RecordStatus.DELETED.code());
            int replyDeletedRows = commentMapper.update(null, markRepliesDeleted);
            if (replyDeletedRows > 0) {
                totalDeletedCount += replyDeletedRows;
            }
        }

        increaseVideoCommentCount(comment.getVideoId(), -totalDeletedCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long uid, Long commentId) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        if (commentId == null || commentId <= 0) {
            throw new IllegalArgumentException("commentId is invalid");
        }

        ensureCommentExists(commentId);

        QueryWrapper<CommentLikeDO> query = new QueryWrapper<>();
        query.eq("comment_id", commentId)
                .eq("user_id", uid);
        CommentLikeDO relation = commentLikeMapper.selectOne(query);

        if (relation == null) {
            CommentLikeDO newRelation = new CommentLikeDO();
            newRelation.setCommentId(commentId);
            newRelation.setUserId(uid);
            newRelation.setStatus(RecordStatus.NORMAL.code());
            int insertRows = commentLikeMapper.insert(newRelation);
            if (insertRows != 1) {
                throw new RuntimeException("insert comment like relation failed");
            }
            increaseCommentLikeCount(commentId);
            return;
        }

        if (RecordStatus.NORMAL.matches(relation.getStatus())) {
            return;
        }

        UpdateWrapper<CommentLikeDO> reactivate = new UpdateWrapper<>();
        reactivate.eq("id", relation.getId())
                .eq("status", RecordStatus.DELETED.code())
                .set("status", RecordStatus.NORMAL.code());
        int reactivateRows = commentLikeMapper.update(null, reactivate);
        if (reactivateRows != 1) {
            return;
        }
        increaseCommentLikeCount(commentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long uid, Long commentId) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        if (commentId == null || commentId <= 0) {
            throw new IllegalArgumentException("commentId is invalid");
        }

        ensureCommentExists(commentId);

        UpdateWrapper<CommentLikeDO> cancel = new UpdateWrapper<>();
        cancel.eq("comment_id", commentId)
                .eq("user_id", uid)
                .eq("status", RecordStatus.NORMAL.code())
                .set("status", RecordStatus.DELETED.code());
        int updateRows = commentLikeMapper.update(null, cancel);
        if (updateRows == 0) {
            return;
        }
        if (updateRows != 1) {
            throw new RuntimeException("cancel comment like relation failed");
        }

        decreaseCommentLikeCount(commentId);
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

    private void ensureCommentExists(Long commentId) {
        LambdaQueryWrapper<CommentDO> query = new LambdaQueryWrapper<>();
        query.eq(CommentDO::getId, commentId)
                .eq(CommentDO::getStatus, RecordStatus.NORMAL.code());
        Long count = commentMapper.selectCount(query);
        if (count == null || count <= 0) {
            throw new IllegalArgumentException("comment not found");
        }
    }

    private void increaseVideoCommentCount(Long videoId, int delta) {
        if (delta == 0) {
            return;
        }
        LambdaUpdateWrapper<VideoDO> update = new LambdaUpdateWrapper<>();
        update.eq(VideoDO::getId, videoId)
                .eq(VideoDO::getStatus, RecordStatus.NORMAL.code());
        if (delta > 0) {
            update.setSql("comment_count = comment_count + " + delta);
        } else {
            update.setSql("comment_count = GREATEST(comment_count - " + (-delta) + ", 0)");
        }
        int rows = videoMapper.update(null, update);
        if (rows != 1) {
            throw new RuntimeException("update video comment_count failed");
        }
    }

    private void increaseParentReplyCount(Long parentId, int delta) {
        if (delta == 0) {
            return;
        }
        LambdaUpdateWrapper<CommentDO> update = new LambdaUpdateWrapper<>();
        update.eq(CommentDO::getId, parentId)
                .eq(CommentDO::getStatus, RecordStatus.NORMAL.code());
        if (delta > 0) {
            update.setSql("reply_count = reply_count + " + delta);
        } else {
            update.setSql("reply_count = GREATEST(reply_count - " + (-delta) + ", 0)");
        }
        commentMapper.update(null, update);
    }

    private void increaseCommentLikeCount(Long commentId) {
        LambdaUpdateWrapper<CommentDO> update = new LambdaUpdateWrapper<>();
        update.eq(CommentDO::getId, commentId)
                .eq(CommentDO::getStatus, RecordStatus.NORMAL.code())
                .setSql("like_count = like_count + 1");
        int rows = commentMapper.update(null, update);
        if (rows != 1) {
            throw new RuntimeException("increase comment like_count failed");
        }
    }

    private void decreaseCommentLikeCount(Long commentId) {
        LambdaUpdateWrapper<CommentDO> update = new LambdaUpdateWrapper<>();
        update.eq(CommentDO::getId, commentId)
                .eq(CommentDO::getStatus, RecordStatus.NORMAL.code())
                .setSql("like_count = GREATEST(like_count - 1, 0)");
        int rows = commentMapper.update(null, update);
        if (rows != 1) {
            throw new RuntimeException("decrease comment like_count failed");
        }
    }

    private Map<Long, UserInfoDO> queryUserInfos(List<CommentDO> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> userIds = comments.stream()
                .map(CommentDO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<UserInfoDO> query = new LambdaQueryWrapper<>();
        query.in(UserInfoDO::getUserId, userIds);
        List<UserInfoDO> userInfos = userInfoMapper.selectList(query);
        if (userInfos == null || userInfos.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, UserInfoDO> map = new HashMap<>();
        for (UserInfoDO info : userInfos) {
            map.put(info.getUserId(), info);
        }
        return map;
    }

    private Set<Long> queryLikedCommentIds(List<CommentDO> comments, Long currentUid) {
        if (currentUid == null || currentUid <= 0 || comments == null || comments.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> commentIds = comments.stream()
                .map(CommentDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (commentIds.isEmpty()) {
            return Collections.emptySet();
        }

        LambdaQueryWrapper<CommentLikeDO> query = new LambdaQueryWrapper<>();
        query.eq(CommentLikeDO::getUserId, currentUid)
                .eq(CommentLikeDO::getStatus, RecordStatus.NORMAL.code())
                .in(CommentLikeDO::getCommentId, commentIds);
        List<CommentLikeDO> likes = commentLikeMapper.selectList(query);
        if (likes == null || likes.isEmpty()) {
            return Collections.emptySet();
        }
        return likes.stream()
                .map(CommentLikeDO::getCommentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static CommentVO buildCommentVO(CommentDO comment, UserInfoDO userInfo, Set<Long> likedCommentIds) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setVideoId(comment.getVideoId());
        vo.setUid(comment.getUserId());
        vo.setParentId(comment.getParentId());
        vo.setRootId(comment.getRootId());
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikeCount() == null ? 0L : comment.getLikeCount());
        vo.setReplyCount(comment.getReplyCount() == null ? 0 : comment.getReplyCount());
        vo.setCreateTime(comment.getCreateTime());
        if (userInfo != null) {
            vo.setNickname(userInfo.getNickname());
            vo.setAvatar(userInfo.getAvatarUrl());
        }
        vo.setIsLiked(likedCommentIds != null && likedCommentIds.contains(comment.getId()));
        vo.setChildComments(new ArrayList<>());
        return vo;
    }

}
