package com.mall.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.marketing.entity.Activity;
import com.mall.marketing.entity.ActivitySku;
import com.mall.marketing.mapper.ActivityMapper;
import com.mall.marketing.mapper.ActivitySkuMapper;
import com.mall.marketing.service.ActivityService;
import com.mall.common.exception.BusinessException;
import com.mall.product.entity.Sku;
import com.mall.product.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {
    private final ActivityMapper activityMapper;
    private final ActivitySkuMapper activitySkuMapper;
    private final SkuMapper skuMapper;

    @Override
    public IPage<Activity> selectPage(Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<Activity> wrapper = Wrappers.<Activity>lambdaQuery()
                .eq(status != null, Activity::getStatus, status)
                .orderByDesc(Activity::getCreateTime);
        return activityMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Activity getById(Long id) {
        return activityMapper.selectById(id);
    }

    @Override
    public void save(Activity activity) {
        if (!StringUtils.hasText(activity.getActivityType())) activity.setActivityType("DISCOUNT");
        activity.setStatus(0);
        activityMapper.insert(activity);
    }

    @Override
    public void update(Activity activity) {
        activityMapper.updateById(activity);
    }

    @Override
    public void delete(Long id) {
        activityMapper.deleteById(id);
    }

    @Override
    public List<Activity> selectActive() {
        LocalDateTime now = LocalDateTime.now();
        List<Activity> activities = activityMapper.selectList(Wrappers.<Activity>lambdaQuery()
                .le(Activity::getStartTime, now).ge(Activity::getEndTime, now)
                .ne(Activity::getStatus, 2).orderByAsc(Activity::getEndTime));
        if (activities.isEmpty()) return activities;
        Map<Long, List<ActivitySku>> items = activitySkuMapper.selectList(
                        Wrappers.<ActivitySku>lambdaQuery().in(ActivitySku::getActivityId,
                                activities.stream().map(Activity::getId).toList()))
                .stream().collect(Collectors.groupingBy(ActivitySku::getActivityId));
        activities.forEach(activity -> activity.setSkuItems(items.getOrDefault(activity.getId(), List.of())));
        return activities;
    }

    @Override
    public List<ActivitySku> listSkus(Long activityId) {
        return activitySkuMapper.selectList(Wrappers.<ActivitySku>lambdaQuery()
                .eq(ActivitySku::getActivityId, activityId).orderByAsc(ActivitySku::getId));
    }

    @Override
    public void saveSku(Long activityId, ActivitySku activitySku) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activitySku == null || activitySku.getSkuId() == null) {
            throw new BusinessException("活动或商品不存在");
        }
        Sku sku = skuMapper.selectById(activitySku.getSkuId());
        if (sku == null || !Integer.valueOf(1).equals(sku.getStatus())) throw new BusinessException("商品不存在或已下架");
        if (activitySku.getSeckillPrice() != null && activitySku.getSeckillPrice().signum() < 0) {
            throw new BusinessException("活动价不能为负数");
        }
        if (activitySku.getSeckillPrice() != null && sku.getPrice() != null
                && activitySku.getSeckillPrice().compareTo(sku.getPrice()) > 0) {
            throw new BusinessException("活动价不能高于商品原价");
        }
        if (activitySku.getSeckillStock() != null && activitySku.getSeckillStock() <= 0) {
            throw new BusinessException("活动库存必须大于 0");
        }
        if ("SECKILL".equals(activity.getActivityType())
                && (activitySku.getSeckillPrice() == null || activitySku.getSeckillStock() == null)) {
            throw new BusinessException("秒杀活动必须配置活动价和活动库存");
        }
        if (activitySku.getLimitPerUser() == null) activitySku.setLimitPerUser(1);
        if (activitySku.getLimitPerUser() <= 0) throw new BusinessException("每人限购必须大于 0");
        ActivitySku existing = activitySkuMapper.selectOne(Wrappers.<ActivitySku>lambdaQuery()
                .eq(ActivitySku::getActivityId, activityId).eq(ActivitySku::getSkuId, activitySku.getSkuId()));
        activitySku.setActivityId(activityId);
        if (existing == null) activitySkuMapper.insert(activitySku);
        else { activitySku.setId(existing.getId()); activitySkuMapper.updateById(activitySku); }
    }

    @Override
    public void deleteSku(Long activityId, Long skuId) {
        activitySkuMapper.delete(Wrappers.<ActivitySku>lambdaQuery()
                .eq(ActivitySku::getActivityId, activityId).eq(ActivitySku::getSkuId, skuId));
    }

    @Override
    public ActivitySku findActiveSku(Long skuId) {
        return activitySkuMapper.findActiveBySkuId(skuId, LocalDateTime.now());
    }

    @Override
    public boolean reserveStock(ActivitySku activitySku, int quantity) {
        return activitySku == null || activitySku.getSeckillStock() == null
                || activitySkuMapper.reserveStock(activitySku.getActivityId(), activitySku.getSkuId(), quantity) == 1;
    }

    @Override
    public void releaseStock(Long activityId, Long skuId, int quantity) {
        if (activitySkuMapper.releaseStock(activityId, skuId, quantity) != 1) {
            throw new BusinessException("活动库存状态异常");
        }
    }
}
