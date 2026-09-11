package com.mall.product.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BusinessException;
import com.mall.common.service.FileService;
import com.mall.product.entity.Category;
import com.mall.product.entity.Sku;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.product.service.StoreCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StoreCatalogServiceImpl implements StoreCatalogService {
    private final CategoryMapper categoryMapper;
    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private FileService fileService;

    @Override
    public List<Category> categories() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(Category::getStatus, 1).orderByAsc(Category::getOrderNum));
    }

    @Override
    public IPage<?> products(int page, int size, Long categoryId, String keyword) {
        return products(page, size, categoryId, keyword, null, null, "sales");
    }

    @Override
    public IPage<?> products(int page, int size, Long categoryId, String keyword,
                             BigDecimal minPrice, BigDecimal maxPrice, String sortField) {
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0) {
            throw new BusinessException("价格不能为负数");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException("最低价不能高于最高价");
        }
        String selectedSort = switch (sortField == null ? "" : sortField) {
            case "priceAsc", "priceDesc", "newest" -> sortField;
            default -> "sales";
        };
        IPage<Spu> result = spuMapper.selectStorePage(
                new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 50)),
                categoryId, keyword, minPrice, maxPrice, selectedSort);
        if (result.getRecords().isEmpty()) return result;

        List<Long> spuIds = result.getRecords().stream().map(Spu::getId).toList();
        Map<Long, Sku> cards = new HashMap<>();
        skuMapper.selectList(Wrappers.<Sku>lambdaQuery().in(Sku::getSpuId, spuIds).eq(Sku::getStatus, 1))
                .forEach(sku -> {
                    Sku current = cards.get(sku.getSpuId());
                    if (sku.getPrice() != null && (current == null || sku.getPrice().compareTo(current.getPrice()) < 0)) {
                        cards.put(sku.getSpuId(), sku);
                    }
                });
        result.getRecords().forEach(spu -> {
            Sku sku = cards.get(spu.getId());
            if (sku != null) {
                spu.setMinPrice(sku.getPrice());
                spu.setCurrency(StringUtils.hasText(sku.getCurrency()) ? sku.getCurrency() : "USD");
                spu.setCoverImage(firstImage(sku.getImages()));
            }
        });
        return result;
    }

    private String firstImage(String images) {
        if (!StringUtils.hasText(images)) return null;
        try {
            List<String> values = objectMapper.readValue(images, new TypeReference<>() {});
            if (values.isEmpty()) return null;
            String image = values.get(0);
            return image != null && !image.startsWith("http") && image.contains("/") && fileService != null
                    ? fileService.getPresignedUrl(image) : image;
        } catch (Exception ignored) {
            return null;
        }
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
