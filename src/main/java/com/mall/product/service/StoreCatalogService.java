package com.mall.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.product.entity.Category;

import java.util.List;
import java.util.Map;

/** 面向 C 端的只读商品目录，不复用后台商品管理接口。 */
public interface StoreCatalogService {
    List<Category> categories();
    IPage<?> products(int page, int size, Long categoryId, String keyword);
    Map<String, Object> detail(Long spuId);
}
