package com.mall.web.controller.product;

import com.mall.common.result.Result;
import com.mall.product.entity.Category;
import com.mall.product.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result<Object> tree() {
        return Result.success(categoryService.selectTree());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result<Category> get(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result<Void> add(@Valid @RequestBody Category category) {
        categoryService.save(category);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Category category) {
        category.setId(id);
        categoryService.update(category);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success(null);
    }
}
