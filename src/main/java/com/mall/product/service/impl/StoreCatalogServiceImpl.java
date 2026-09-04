package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BusinessException;
import com.mall.product.entity.Category;
import com.mall.product.entity.Sku;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.product.service.StoreCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StoreCatalogServiceImpl implements StoreCatalogService {
    private final CategoryMapper categoryMapper;
    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;

    @Override
    public List<Category> categories() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(Category::getStatus, 1).orderByAsc(Category::getOrderNum));
    }

    @Override
    public IPage<?> products(int page, int size, Long categoryId, String keyword) {
        return spuMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 50)),
                Wrappers.<Spu>lambdaQuery().eq(Spu::getStatus, 1)
                        .eq(categoryId != null, Spu::getCategoryId, categoryId)
                        .like(StringUtils.hasText(keyword), Spu::getSpuName, keyword)
                        .orderByDesc(Spu::getSalesCount).orderByDesc(Spu::getCreateTime));
    }

    @Override
    public Map<String, Object> detail(Long spuId) {
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null || !Integer.valueOf(1).equals(spu.getStatus())) {
            throw new BusinessException("商品不存在或已下架");
        }
        List<Sku> skus = skuMapper.selectList(Wrappers.<Sku>lambdaQuery()
                .eq(Sku::getSpuId, spuId).eq(Sku::getStatus, 1));
        return Map.of("spu", spu, "skus", skus);
    }
}
