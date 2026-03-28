package com.bilibili.access.service;

import com.bilibili.access.model.state.UserAccessState;

public interface UserAccessService {

    UserAccessState getUserAccessState(Long userId);

    boolean canLike(Long userId);

    boolean canComment(Long userId);

    boolean canSendImMessage(Long userId);

    boolean canUploadVideo(Long userId);

    boolean canEditProfile(Long userId);

    void validateCanLike(Long userId);

    void validateCanComment(Long userId);

    void validateCanSendImMessage(Long userId);

    void validateCanUploadVideo(Long userId);

    void validateCanEditProfile(Long userId);
}
