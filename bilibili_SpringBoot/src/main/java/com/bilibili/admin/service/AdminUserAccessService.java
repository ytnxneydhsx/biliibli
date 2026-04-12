package com.bilibili.admin.service;

import com.bilibili.admin.model.vo.AdminUserVO;

public interface AdminUserAccessService {

    AdminUserVO banVideoBusiness(Long userId, Long operatorId);

    AdminUserVO unbanVideoBusiness(Long userId, Long operatorId);
}
