package com.mall.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.system.vo.SysRoleVO;

public interface SysRoleService {
    IPage<SysRoleVO> selectRolePage(Integer page, Integer size);
}
