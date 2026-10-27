package com.mall.web.controller.product;

import com.mall.common.result.Result;
import com.mall.product.entity.Brand;
import com.mall.product.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 提供运营后台商品品牌的分页查询和维护入口。 */
@RestController
@RequestMapping("/api/product/brand")
@RequiredArgsConstructor
public class BrandController {
    /** 处理商品品牌的检索及维护。 */
    private final BrandService brandService;

    /** 分页查询品牌，可选使用关键字筛选。 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                String keyword) {
        return Result.success(brandService.selectPage(page, size, keyword));
    }

    /** 按品牌 ID 查询品牌详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result<Brand> get(@PathVariable Long id) {
        return Result.success(brandService.getById(id));
    }

    /** 新增品牌；请求字段由 Bean Validation 校验。 */
    @PostMapping
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result<Void> add(@Valid @RequestBody Brand brand) {
        brandService.save(brand);
        return Result.success(null);
    }

    /** 更新品牌；路径 ID 覆盖请求体中的实体 ID。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Brand brand) {
        brand.setId(id);
        brandService.update(brand);
        return Result.success(null);
    }

    /** 删除指定品牌，关联数据限制由品牌服务校验。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return Result.success(null);
    }
}
