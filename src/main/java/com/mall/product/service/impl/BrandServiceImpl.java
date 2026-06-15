package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.product.entity.Brand;
import com.mall.product.mapper.BrandMapper;
import com.mall.product.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {
    private final BrandMapper brandMapper;

    @Override
    public IPage<Brand> selectPage(Integer page, Integer size, String keyword) {
        LambdaQueryWrapper<Brand> wrapper = Wrappers.<Brand>lambdaQuery()
                .like(StringUtils.hasText(keyword), Brand::getBrandName, keyword)
                .orderByAsc(Brand::getOrderNum);
        return brandMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Brand getById(Long id) {
        return brandMapper.selectById(id);
    }

    @Override
    public void save(Brand brand) {
        brandMapper.insert(brand);
    }

    @Override
    public void update(Brand brand) {
        brandMapper.updateById(brand);
    }

    @Override
    public void delete(Long id) {
        brandMapper.deleteById(id);
    }
}
