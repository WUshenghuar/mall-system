package com.mall.web.controller.store;

import com.mall.common.result.Result;
import com.mall.product.entity.Category;
import com.mall.product.service.StoreCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreProductController {
    private final StoreCatalogService storeCatalogService;

    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.success(storeCatalogService.categories());
    }

    @GetMapping("/products")
    public Result<?> products(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(required = false) Long categoryId,
                              @RequestParam(required = false) String keyword) {
        return Result.success(storeCatalogService.products(page, size, categoryId, keyword));
    }

    @GetMapping("/products/{spuId}")
    public Result<?> detail(@PathVariable Long spuId) {
        return Result.success(storeCatalogService.detail(spuId));
    }
}
