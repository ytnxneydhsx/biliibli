package com.bilibili.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.admin.mapper.AdminUserMapper;
import com.bilibili.admin.model.vo.AdminUserVO;
import com.bilibili.admin.service.AdminUserService;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.common.page.PageVO;
import com.bilibili.tool.StringTool;
import org.springframework.stereotype.Service;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserMapper adminUserMapper;

    public AdminUserServiceImpl(AdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    public PageVO<AdminUserVO> listUsers(PageQueryDTO pageQuery, String keyword) {
        PageQueryDTO query = pageQuery == null ? new PageQueryDTO() : pageQuery;
        String normalizedKeyword = StringTool.normalizeOptional(keyword);
        Page<AdminUserVO> page = new Page<>(query.normalizedPageNo(), query.normalizedPageSize());
        return PageVO.from(adminUserMapper.selectAdminUsers(page, normalizedKeyword, parseUidKeyword(normalizedKeyword)));
    }

    private Long parseUidKeyword(String keyword) {
        if (keyword == null || !keyword.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Long.valueOf(keyword);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
