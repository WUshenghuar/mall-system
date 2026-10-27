package com.mall.web.controller.store;

import com.mall.common.result.Result;
import com.mall.product.entity.Category;
import com.mall.product.service.StoreCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.math.BigDecimal;

/** 提供 C 端商品分类、筛选列表和商品详情查询接口。 */
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreProductController {
    /** 提供面向 C 端的分类和商品目录查询。 */
    private final StoreCatalogService storeCatalogService;

    /** 查询 C 端展示的商品分类。 */
    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.success(storeCatalogService.categories());
    }

    /**
     * 分页查询商品目录，可按分类、关键字及价格区间筛选。
     *
     * <p>页码默认 1、每页默认 20 条；排序字段默认按销量排序。</p>
     */
    @GetMapping("/products")
    public Result<?> products(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(required = false) Long categoryId,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) BigDecimal minPrice,
                              @RequestParam(required = false) BigDecimal maxPrice,
                              @RequestParam(defaultValue = "sales") String sortField) {
        return Result.success(storeCatalogService.products(page, size, categoryId, keyword, minPrice, maxPrice, sortField));
    }

    /** 查询指定 SPU 的 C 端商品详情。 */
    @GetMapping("/products/{spuId}")
    public Result<?> detail(@PathVariable Long spuId) {
        return Result.success(storeCatalogService.detail(spuId));
    }
}
