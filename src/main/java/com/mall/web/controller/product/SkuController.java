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

/** 提供运营后台 SKU 管理、批量改价和库存查询入口。 */
@RestController
@RequestMapping("/api/product/sku")
@RequiredArgsConstructor
public class SkuController {
    /** 处理 SKU 查询、维护、批量改价及库存读写。 */
    private final SkuService skuService;

    /** 查询指定 SPU 下的 SKU 列表。 */
    @GetMapping("/list/{spuId}")
    @PreAuthorize("hasAuthority('product:spu:list')")
    public Result<List<Sku>> list(@PathVariable Long spuId) {
        return Result.success(skuService.listBySpuId(spuId));
    }

    /** 新增 SKU；实体字段由请求体提供并经 Bean Validation 校验。 */
    @PostMapping
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Void> add(@Valid @RequestBody Sku sku) {
        skuService.save(sku);
        return Result.success(null);
    }

    /** 更新指定 SKU；路径 ID 覆盖请求体实体 ID。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Sku sku) {
        sku.setId(id);
        skuService.update(sku);
        return Result.success(null);
    }

    /** 删除指定 SKU，需要商品删除权限。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        skuService.delete(id);
        return Result.success(null);
    }

    /** 对请求中的 SKU 集合批量设置统一价格。 */
    @PutMapping("/price")
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Void> batchPrice(@RequestBody BatchPriceReq req) {
        skuService.batchUpdatePrice(req.getSkuIds(), req.getPrice());
        return Result.success(null);
    }

    /** 更新指定 SKU 的库存数量。 */
    @PutMapping("/stock/{skuId}")
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Void> stock(@PathVariable Long skuId, @RequestParam Integer stock) {
        skuService.updateStock(skuId, stock);
        return Result.success(null);
    }

    /** 查询指定 SKU 的库存数量。 */
    @GetMapping("/stock/{skuId}")
    @PreAuthorize("hasAuthority('product:spu:list')")
    public Result<Integer> getStock(@PathVariable Long skuId) {
        return Result.success(skuService.getStock(skuId));
    }

    /** 批量改价请求；包含 SKU ID 列表和要设置的价格。 */
    @Data
    public static class BatchPriceReq {
        private List<Long> skuIds;
        private BigDecimal price;
    }
}
