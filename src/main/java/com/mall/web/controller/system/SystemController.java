package com.mall.web.controller.system;

import com.mall.common.result.Result;
import com.mall.system.mapper.SysRoleMapper;
import com.mall.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    @GetMapping("/user/list")
    public Result<List<Map<String, Object>>> userList() {
        List<Map<String, Object>> list = userMapper.selectList(null).stream().map(u -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("realName", u.getRealName());
            map.put("email", u.getEmail());
            map.put("phone", u.getPhone());
            map.put("status", u.getStatus());
            map.put("roleKeys", userMapper.selectRoleKeysByUserId(u.getId()));
            return map;
        }).collect(Collectors.toList());
        return Result.success(list);
    }

    @GetMapping("/role/list")
    public Result<List<com.mall.system.entity.SysRole>> roleList() {
        return Result.success(roleMapper.selectList(null));
    }
}
