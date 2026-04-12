package com.bilibili.admin.controller;

import com.bilibili.admin.model.vo.AdminUserVO;
import com.bilibili.admin.service.AdminUserService;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.common.page.PageVO;
import com.bilibili.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - User", description = "管理员用户查询接口")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "分页查询所有用户及权限状态")
    public Result<PageVO<AdminUserVO>> listUsers(PageQueryDTO pageQuery) {
        return Result.success(adminUserService.listUsers(pageQuery));
    }
}
