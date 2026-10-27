package com.mall.web.controller.product;

import com.mall.common.result.Result;
import com.mall.product.entity.Spu;
import com.mall.product.service.SpuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 提供运营后台 SPU 查询、维护、上下架和搜索索引联动入口。 */
@RestController
@RequestMapping("/api/product/spu")
@RequiredArgsConstructor
public class SpuController {
    /** 处理 SPU 商品查询、维护及发布状态变更。 */
    private final SpuService spuService;

    /** 分页查询 SPU，可按分类、状态和关键字筛选。 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('product:spu:list')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                Long categoryId, Integer status, String keyword) {
        return Result.success(spuService.selectPage(page, size, categoryId, status, keyword));
    }

    /** 按 SPU ID 查询商品详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:list')")
    public Result<Spu> get(@PathVariable Long id) {
        return Result.success(spuService.getById(id));
    }

    /** 新增 SPU；商品字段经 Bean Validation 校验。 */
    @PostMapping
    @PreAuthorize("hasAuthority('product:spu:add')")
    public Result<Void> add(@Valid @RequestBody Spu spu) {
        spuService.save(spu);
        return Result.success(null);
    }

    /** 更新指定 SPU；路径 ID 作为目标记录 ID。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Spu spu) {
        spu.setId(id);
        spuService.update(spu);
        return Result.success(null);
    }

    /** 删除指定 SPU，关联 SKU 等限制由商品服务处理。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        spuService.delete(id);
        return Result.success(null);
    }

    /** 将指定 SPU 发布或下架，目标状态由 status 参数传入。 */
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('product:spu:publish')")
    public Result<Void> publish(@PathVariable Long id, @RequestParam Integer status) {
        spuService.publish(id, status);
        return Result.success(null);
    }
}
