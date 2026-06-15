package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BusinessException;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.SpuMapper;
import com.mall.product.service.SpuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SpuServiceImpl implements SpuService {
    private final SpuMapper spuMapper;

    @Override
    public IPage<Spu> selectPage(Integer page, Integer size, Long categoryId,
                                  Integer status, String keyword) {
        LambdaQueryWrapper<Spu> wrapper = Wrappers.<Spu>lambdaQuery()
                .eq(categoryId != null, Spu::getCategoryId, categoryId)
                .eq(status != null, Spu::getStatus, status)
                .like(StringUtils.hasText(keyword), Spu::getSpuName, keyword)
                .orderByDesc(Spu::getCreateTime);
        return spuMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Spu getById(Long id) {
        return spuMapper.selectById(id);
    }

    @Override
    public void save(Spu spu) {
        spuMapper.insert(spu);
    }

    @Override
    public void update(Spu spu) {
        spuMapper.updateById(spu);
    }

    @Override
    public void delete(Long id) {
        spuMapper.deleteById(id);
    }

    @Override
    public void publish(Long id, Integer status) {
        Spu spu = spuMapper.selectById(id);
        if (spu == null) {
            throw new BusinessException("SPU不存在");
        }
        spu.setStatus(status);
        spuMapper.updateById(spu);
    }
}
