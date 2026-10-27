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

/** 提供后台用户和角色查询入口，并通过方法级权限限制访问范围。 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {
    /** 提供后台用户分页查询。 */
    private final SysUserService userService;
    /** 提供系统角色查询。 */
    private final SysRoleService roleService;

    /** 分页查询系统用户，需要系统用户列表权限。 */
    @GetMapping("/user/list")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Object> userList(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(userService.selectUserPage(page, size));
    }

    /** 查询角色列表，当前接口返回第一页、最多 100 条角色记录。 */
    @GetMapping("/role/list")
    @PreAuthorize("hasAuthority('system:role:config')")
    public Result<List<SysRoleVO>> roleList() {
        return Result.success(roleService.selectRolePage(1, 100).getRecords());
    }
}
