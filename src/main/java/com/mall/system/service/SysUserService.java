package com.mall.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

public interface SysUserService {
    IPage<Map<String, Object>> selectUserPage(Integer page, Integer size);
    List<String> getRoleKeys(Long userId);
}
