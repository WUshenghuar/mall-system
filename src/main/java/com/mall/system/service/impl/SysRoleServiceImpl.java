package com.mall.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.system.entity.SysRole;
import com.mall.system.mapper.SysRoleMapper;
import com.mall.system.service.SysRoleService;
import com.mall.system.vo.SysRoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** 提供后台角色分页数据，并将实体转换为接口展示对象。 */
/**
 * 系统角色服务实现类
 * 实现了SysRoleService接口，提供角色相关的业务逻辑处理
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {
    // 使用final修饰的SysRoleMapper依赖，通过构造器注入
    private final SysRoleMapper roleMapper;

    @Override
    public IPage<SysRoleVO> selectRolePage(Integer page, Integer size) {
        IPage<SysRole> rolePage = roleMapper.selectPage(new Page<>(page, size), null);

        List<SysRoleVO> vos = rolePage.getRecords().stream().map(r -> {
            SysRoleVO vo = new SysRoleVO();
            vo.setId(r.getId());
            vo.setRoleName(r.getRoleName());
            vo.setRoleKey(r.getRoleKey());
            vo.setRoleSort(r.getRoleSort());
            vo.setStatus(r.getStatus());
            vo.setRemark(r.getRemark());
            return vo;
        }).collect(Collectors.toList());

        IPage<SysRoleVO> result = new Page<>(page, size, rolePage.getTotal());
        result.setRecords(vos);
        return result;
    }
}
