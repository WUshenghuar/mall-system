package com.mall.web.controller.product;

import com.mall.common.result.Result;
import com.mall.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class SearchController {
    private final ProductSearchService searchService;

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

    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Void> sync() {
        searchService.syncAllProducts();
        return Result.success(null);
    }
}
