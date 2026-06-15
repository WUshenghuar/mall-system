package com.mall.web.controller.marketing;

import com.mall.common.result.Result;
import com.mall.marketing.entity.Activity;
import com.mall.marketing.service.ActivityService;
import com.mall.marketing.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/marketing/activity")
@RequiredArgsConstructor
public class ActivityController {
    private final ActivityService activityService;
    private final SeckillService seckillService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                Integer status) {
        return Result.success(activityService.selectPage(page, size, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Activity> get(@PathVariable Long id) {
        return Result.success(activityService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Void> add(@RequestBody Activity activity) {
        activityService.save(activity);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Activity activity) {
        activity.setId(id);
        activityService.update(activity);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/seckill/prepare")
    @PreAuthorize("hasAuthority('marketing:activity:config')")
    public Result<Void> prepareSeckill(@PathVariable Long id,
                                        @RequestParam Long skuId,
                                        @RequestParam Integer totalStock) {
        seckillService.prepareSeckill(id, skuId, totalStock);
        return Result.success(null);
    }
}
