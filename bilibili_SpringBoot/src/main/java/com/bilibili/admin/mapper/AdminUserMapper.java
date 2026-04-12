package com.bilibili.admin.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.admin.model.vo.AdminUserVO;
import org.apache.ibatis.annotations.Param;

public interface AdminUserMapper {

    IPage<AdminUserVO> selectAdminUsers(Page<AdminUserVO> page);

    AdminUserVO selectAdminUserById(@Param("userId") Long userId);
}
