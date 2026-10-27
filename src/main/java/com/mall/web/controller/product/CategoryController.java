package com.mall.web.controller.product;

import com.mall.common.result.Result;
import com.mall.product.entity.Category;
import com.mall.product.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 提供运营后台商品分类树查询和分类维护入口。 */
@RestController
@RequestMapping("/api/product/category")
@RequiredArgsConstructor
public class CategoryController {
    /** 处理商品分类树查询和分类维护。 */
    private final CategoryService categoryService;

    /** 查询商品分类树结构。 */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result<Object> tree() {
        return Result.success(categoryService.selectTree());
    }

    /** 按分类 ID 查询分类详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result<Category> get(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    /** 新增分类；请求字段由 Bean Validation 校验。 */
    @PostMapping
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result<Void> add(@Valid @RequestBody Category category) {
        categoryService.save(category);
        return Result.success(null);
    }

    /** 更新分类；路径 ID 作为更新目标的主键。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Category category) {
        category.setId(id);
        categoryService.update(category);
        return Result.success(null);
    }

    /** 删除指定分类，层级及关联商品约束由分类服务处理。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success(null);
    }
}
