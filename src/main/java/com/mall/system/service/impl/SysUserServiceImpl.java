package com.mall.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.system.entity.SysUser;
import com.mall.system.mapper.SysUserMapper;
import com.mall.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {
    private final SysUserMapper userMapper;

    @Override
    public IPage<Map<String, Object>> selectUserPage(Integer page, Integer size) {
        IPage<SysUser> userPage = userMapper.selectPage(new Page<>(page, size), null);
        List<SysUser> users = userPage.getRecords();

        // 批量查询角色 — 一次 SQL 替代 N 次
        Map<Long, List<String>> roleMap = Collections.emptyMap();
        if (!users.isEmpty()) {
            List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
            List<Map<String, Object>> rows = userMapper.selectRoleKeysByUserIds(userIds);
            roleMap = rows.stream().collect(Collectors.groupingBy(
                    row -> (Long) row.get("user_id"),
                    Collectors.mapping(row -> (String) row.get("role_key"), Collectors.toList())
            ));
        }

        Map<Long, List<String>> finalRoleMap = roleMap;
        List<Map<String, Object>> list = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("realName", u.getRealName());
            map.put("email", u.getEmail());
            map.put("phone", u.getPhone());
            map.put("status", u.getStatus());
            map.put("roleKeys", finalRoleMap.getOrDefault(u.getId(), Collections.emptyList()));
            return map;
        }).collect(Collectors.toList());

        IPage<Map<String, Object>> result = new Page<>(page, size, userPage.getTotal());
        result.setRecords(list);
        return result;
    }

    @Override
    public List<String> getRoleKeys(Long userId) {
        return userMapper.selectRoleKeysByUserId(userId);
    }
}
