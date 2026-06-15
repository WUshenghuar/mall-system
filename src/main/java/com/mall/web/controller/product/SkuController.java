package com.mall.web.controller.product;

import com.mall.common.result.Result;
import com.mall.product.entity.Sku;
import com.mall.product.service.SkuService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/product/sku")
@RequiredArgsConstructor
public class SkuController {
    private final SkuService skuService;

    @GetMapping("/list/{spuId}")
    @PreAuthorize("hasAuthority('product:sku:list')")
    public Result<List<Sku>> list(@PathVariable Long spuId) {
        return Result.success(skuService.listBySpuId(spuId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Void> add(@Valid @RequestBody Sku sku) {
        skuService.save(sku);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Sku sku) {
        sku.setId(id);
        skuService.update(sku);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        skuService.delete(id);
        return Result.success(null);
    }

    @PutMapping("/price")
    @PreAuthorize("hasAuthority('product:sku:price')")
    public Result<Void> batchPrice(@RequestBody BatchPriceReq req) {
        skuService.batchUpdatePrice(req.getSkuIds(), req.getPrice());
        return Result.success(null);
    }

    @PutMapping("/stock/{skuId}")
    @PreAuthorize("hasAuthority('product:sku:stock')")
    public Result<Void> stock(@PathVariable Long skuId, @RequestParam Integer stock) {
        skuService.updateStock(skuId, stock);
        return Result.success(null);
    }

    @GetMapping("/stock/{skuId}")
    @PreAuthorize("hasAuthority('product:sku:list')")
    public Result<Integer> getStock(@PathVariable Long skuId) {
        return Result.success(skuService.getStock(skuId));
    }

    @Data
    public static class BatchPriceReq {
        private List<Long> skuIds;
        private BigDecimal price;
    }
}
