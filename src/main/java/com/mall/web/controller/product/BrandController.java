package com.mall.web.controller.product;

import com.mall.common.result.Result;
import com.mall.product.entity.Brand;
import com.mall.product.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product/brand")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                String keyword) {
        return Result.success(brandService.selectPage(page, size, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result<Brand> get(@PathVariable Long id) {
        return Result.success(brandService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result<Void> add(@Valid @RequestBody Brand brand) {
        brandService.save(brand);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Brand brand) {
        brand.setId(id);
        brandService.update(brand);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return Result.success(null);
    }
}
