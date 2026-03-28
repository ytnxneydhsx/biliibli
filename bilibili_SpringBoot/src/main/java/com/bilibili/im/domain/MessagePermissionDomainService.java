package com.bilibili.im.domain;

public interface MessagePermissionDomainService {

    void validateCanSendMessage(Long senderId, Long receiverId);
}
