package com.mall.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

public interface SysUserService {
    /** 分页查询后台用户并附带其角色标识。 */
    IPage<Map<String, Object>> selectUserPage(Integer page, Integer size);

    /** 查询用于权限判定的角色标识。 */
    List<String> getRoleKeys(Long userId);
}
