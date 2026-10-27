package com.mall.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.system.vo.SysRoleVO;

public interface SysRoleService {
    /** 分页查询角色并转换为后台展示对象。 */
    IPage<SysRoleVO> selectRolePage(Integer page, Integer size);
}
