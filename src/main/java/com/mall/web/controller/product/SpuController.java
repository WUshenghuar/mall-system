package com.mall.web.controller.product;

import com.mall.common.result.Result;
import com.mall.product.entity.Spu;
import com.mall.product.service.SpuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product/spu")
@RequiredArgsConstructor
public class SpuController {
    private final SpuService spuService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('product:spu:list')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                Long categoryId, Integer status, String keyword) {
        return Result.success(spuService.selectPage(page, size, categoryId, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:list')")
    public Result<Spu> get(@PathVariable Long id) {
        return Result.success(spuService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:spu:add')")
    public Result<Void> add(@Valid @RequestBody Spu spu) {
        spuService.save(spu);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Spu spu) {
        spu.setId(id);
        spuService.update(spu);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        spuService.delete(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('product:spu:publish')")
    public Result<Void> publish(@PathVariable Long id, @RequestParam Integer status) {
        spuService.publish(id, status);
        return Result.success(null);
    }
}
