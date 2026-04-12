package com.bilibili.admin.service;

public interface AdminUserAccessService {

    void banVideoBusiness(Long userId, Long operatorId);

    void unbanVideoBusiness(Long userId, Long operatorId);
}
