package com.mall.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.product.entity.Brand;

public interface BrandService {
    IPage<Brand> selectPage(Integer page, Integer size, String keyword);
    Brand getById(Long id);
    void save(Brand brand);
    void update(Brand brand);
    void delete(Long id);
}
