package com.mall.web.controller.marketing;

import com.mall.common.result.Result;
import com.mall.marketing.entity.Activity;
import com.mall.marketing.entity.ActivitySku;
import com.mall.marketing.service.ActivityService;
import com.mall.marketing.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 运营后台营销活动配置接口。
 *
 * <p>提供活动信息维护、活动关联 SKU 管理及秒杀库存预热入口。
 * 所有接口均要求具备营销活动配置权限，具体业务规则由对应服务执行。</p>
 */
@RestController
@RequestMapping("/api/marketing/activity")
@RequiredArgsConstructor
public class ActivityController {
    /** 处理营销活动及其活动 SKU 的查询和维护。 */
    private final ActivityService activityService;
    /** 初始化秒杀活动的可售库存。 */
    private final SeckillService seckillService;

    /** 分页查询活动列表，可选按活动状态筛选；页码和页大小默认分别为 1 和 10。 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                Integer status) {
        return Result.success(activityService.selectPage(page, size, status));
    }

    /** 按活动 ID 查询活动详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Activity> get(@PathVariable Long id) {
        return Result.success(activityService.getById(id));
    }

    /** 新建营销活动，活动属性由请求体提供。 */
    @PostMapping
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Void> add(@RequestBody Activity activity) {
        activityService.save(activity);
        return Result.success(null);
    }

    /** 更新指定活动；以路径中的 ID 为准设置活动主键后交由服务处理。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Activity activity) {
        activity.setId(id);
        activityService.update(activity);
        return Result.success(null);
    }

    /** 删除指定活动；删除限制及关联数据处理由活动服务负责。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return Result.success(null);
    }

    /** 查询指定活动下已配置的 SKU 列表。 */
    @GetMapping("/{id}/skus")
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Object> skus(@PathVariable Long id) {
        return Result.success(activityService.listSkus(id));
    }

    /** 为指定活动新增或保存一个活动 SKU 配置。 */
    @PostMapping("/{id}/skus")
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Void> saveSku(@PathVariable Long id, @RequestBody ActivitySku activitySku) {
        activityService.saveSku(id, activitySku);
        return Result.success(null);
    }

    /** 从指定活动中移除一个 SKU 配置。 */
    @DeleteMapping("/{id}/skus/{skuId}")
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Void> deleteSku(@PathVariable Long id, @PathVariable Long skuId) {
        activityService.deleteSku(id, skuId);
        return Result.success(null);
    }

    /**
     * 为指定活动和 SKU 预热秒杀库存。
     *
     * <p>活动 ID、SKU ID 和预备库存总量通过路径及请求参数传入；
     * 库存初始化和有效性校验由秒杀服务负责。</p>
     */
    @PostMapping("/{id}/seckill/prepare")
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Void> prepareSeckill(@PathVariable Long id,
                                        @RequestParam Long skuId,
                                        @RequestParam Integer totalStock) {
        seckillService.prepareSeckill(id, skuId, totalStock);
        return Result.success(null);
    }
}
