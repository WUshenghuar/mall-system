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

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {
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
