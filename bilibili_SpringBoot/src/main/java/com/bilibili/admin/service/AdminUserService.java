package com.bilibili.admin.service;

import com.bilibili.admin.model.vo.AdminUserVO;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.common.page.PageVO;

public interface AdminUserService {

    PageVO<AdminUserVO> listUsers(PageQueryDTO pageQuery);
}
