package com.bilibili.access.service.impl;

import com.bilibili.access.cache.UserAccessSnapshotCache;
import com.bilibili.access.cache.UserExistenceCache;
import com.bilibili.access.model.cache.UserAccessSnapshot;
import com.bilibili.access.model.state.UserAccessState;
import com.bilibili.access.service.UserAccessService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class UserAccessServiceImpl implements UserAccessService {

    private final UserExistenceCache userExistenceCache;
    private final UserAccessSnapshotCache userAccessSnapshotCache;

    public UserAccessServiceImpl(UserExistenceCache userExistenceCache,
                                 UserAccessSnapshotCache userAccessSnapshotCache) {
        this.userExistenceCache = userExistenceCache;
        this.userAccessSnapshotCache = userAccessSnapshotCache;
    }

    @Override
    public UserAccessState getUserAccessState(Long userId) {
        UserAccessSnapshot snapshot = userAccessSnapshotCache.get(userId);
        return new UserAccessState(
                snapshot.userId(),
                snapshot.likeEnabled(),
                snapshot.commentEnabled(),
                snapshot.imMessageSendEnabled(),
                snapshot.videoUploadEnabled(),
                snapshot.profileEditEnabled()
        );
    }

    @Override
    public boolean canLike(Long userId) {
        return getUserAccessState(userId).isLikeEnabled();
    }

    @Override
    public boolean canComment(Long userId) {
        return getUserAccessState(userId).isCommentEnabled();
    }

    @Override
    public boolean canSendImMessage(Long userId) {
        return userAccessSnapshotCache.get(userId).imMessageSendEnabled();
    }

    @Override
    public boolean canUploadVideo(Long userId) {
        return getUserAccessState(userId).isVideoUploadEnabled();
    }

    @Override
    public boolean canEditProfile(Long userId) {
        return getUserAccessState(userId).isProfileEditEnabled();
    }

    @Override
    public void validateCanLike(Long userId) {
        if (!canLike(userId)) {
            throw new AccessDeniedException("current user cannot like");
        }
    }

    @Override
    public void validateCanComment(Long userId) {
        if (!canComment(userId)) {
            throw new AccessDeniedException("current user cannot comment");
        }
    }

    @Override
    public void validateCanSendImMessage(Long userId) {
        if (!userExistenceCache.exists(userId)) {
            throw new IllegalArgumentException("sender user not found");
        }
        if (!userAccessSnapshotCache.get(userId).imMessageSendEnabled()) {
            throw new AccessDeniedException("current user cannot send im message");
        }
    }

    @Override
    public void validateCanUploadVideo(Long userId) {
        if (!canUploadVideo(userId)) {
            throw new AccessDeniedException("current user cannot upload video");
        }
    }

    @Override
    public void validateCanEditProfile(Long userId) {
        if (!canEditProfile(userId)) {
            throw new AccessDeniedException("current user cannot edit profile");
        }
    }

}
