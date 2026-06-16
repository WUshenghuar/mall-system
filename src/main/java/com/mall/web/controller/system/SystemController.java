package com.mall.web.controller.system;

import com.mall.common.result.Result;
import com.mall.system.service.SysRoleService;
import com.mall.system.service.SysUserService;
import com.mall.system.vo.SysRoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {
    private final SysUserService userService;
    private final SysRoleService roleService;

    @GetMapping("/user/list")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Object> userList(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(userService.selectUserPage(page, size));
    }

    @GetMapping("/role/list")
    @PreAuthorize("hasAuthority('system:role:config')")
    public Result<List<SysRoleVO>> roleList() {
        return Result.success(roleService.selectRolePage(1, 100).getRecords());
    }
}
