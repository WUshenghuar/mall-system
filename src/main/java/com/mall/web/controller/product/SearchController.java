package com.mall.web.controller.product;

import com.mall.common.result.Result;
import com.mall.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 提供商品搜索查询和受权限保护的全量索引同步入口。 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class SearchController {
    /** 提供商品搜索及搜索索引维护能力。 */
    private final ProductSearchService searchService;

    /**
     * 按关键字、分类、价格区间和排序条件搜索商品。
     *
     * <p>所有筛选项均可选；页码默认从 1 开始，每页默认返回 20 条。
     * 控制器仅在 Elasticsearch 功能配置开启时注册。</p>
     */
    @GetMapping("/product")
    public Result<Object> search(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Long categoryId,
                                  @RequestParam(required = false) Double minPrice,
                                  @RequestParam(required = false) Double maxPrice,
                                  @RequestParam(required = false) String sortField,
                                  @RequestParam(required = false) String sortOrder,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return Result.success(searchService.search(keyword, categoryId,
                minPrice, maxPrice, sortField, sortOrder, page, size));
    }

    /** 触发商品全量索引同步；操作受商品编辑权限保护。 */
    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Void> sync() {
        searchService.syncAllProducts();
        return Result.success(null);
    }
}
