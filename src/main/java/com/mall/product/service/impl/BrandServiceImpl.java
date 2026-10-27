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

/** 提供商品品牌的分页查询和基础维护。 */
/**
 * 品牌服务实现类
 * 使用@Service注解标记为服务层组件
 * 使用@RequiredArgsConstructor生成基于final字段的构造器注入
 */
@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {
    // 使用final确保brandMapper不可变，通过构造器注入
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
