package com.bilibili.access.service.impl;

import com.bilibili.access.mapper.UserAccessMapper;
import com.bilibili.access.model.entity.UserAccessDO;
import com.bilibili.access.model.state.UserAccessState;
import com.bilibili.access.service.UserAccessService;
import com.bilibili.user.mapper.UserMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class UserAccessServiceImpl implements UserAccessService {

    private static final int ENABLED = 1;

    private final UserAccessMapper userAccessMapper;
    private final UserMapper userMapper;

    public UserAccessServiceImpl(UserAccessMapper userAccessMapper,
                                 UserMapper userMapper) {
        this.userAccessMapper = userAccessMapper;
        this.userMapper = userMapper;
    }

    @Override
    public UserAccessState getUserAccessState(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }

        UserAccessDO userAccess = userAccessMapper.selectByUserId(userId);
        if (userAccess == null) {
            return buildDefaultState(userId);
        }

        return new UserAccessState(
                userId,
                isEnabled(userAccess.getLikeEnabled()),
                isEnabled(userAccess.getCommentEnabled()),
                isEnabled(userAccess.getImMessageSendEnabled()),
                isEnabled(userAccess.getVideoUploadEnabled()),
                isEnabled(userAccess.getProfileEditEnabled())
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
        return getUserAccessState(userId).isImMessageSendEnabled();
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
        if (userMapper.selectById(userId) == null) {
            throw new IllegalArgumentException("sender user not found");
        }
        if (!canSendImMessage(userId)) {
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

    private UserAccessState buildDefaultState(Long userId) {
        return new UserAccessState(userId, true, true, true, true, true);
    }

    private boolean isEnabled(Integer value) {
        return value != null && value == ENABLED;
    }
}
