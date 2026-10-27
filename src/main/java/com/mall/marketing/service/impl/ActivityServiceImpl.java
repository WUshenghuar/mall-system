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

/** 管理营销活动及其 SKU 规则，并提供活动库存的预留与释放操作。 */
/**
 * 活动服务实现类
 * 实现了活动相关的业务逻辑，包括活动查询、增删改查、活动SKU管理等
 */
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {
    // 活动数据访问层
    private final ActivityMapper activityMapper;
    // 活动SKU数据访问层
    private final ActivitySkuMapper activitySkuMapper;
    // SKU数据访问层
    private final SkuMapper skuMapper;

    @Override
    public IPage<Activity> selectPage(Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<Activity> wrapper = Wrappers.<Activity>lambdaQuery()
                .eq(status != null, Activity::getStatus, status)
                .orderByDesc(Activity::getCreateTime);
        return activityMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 根据ID获取活动信息
     * @param id 活动的唯一标识符
     * @return 返回对应ID的活动对象，如果不存在则返回null
     */
    @Override
    public Activity getById(Long id) {
        // 调用activityMapper的selectById方法，根据id查询数据库并返回活动对象
        return activityMapper.selectById(id);
    }

    @Override
    public void save(Activity activity) {
        if (!StringUtils.hasText(activity.getActivityType())) activity.setActivityType("DISCOUNT");
        activity.setStatus(0);
        activityMapper.insert(activity);
    }

    /**
     * 更新活动信息的方法
     * @param activity 包含更新后活动信息的Activity对象
     */
    @Override
    public void update(Activity activity) {
        // 调用activityMapper的updateById方法，根据活动ID更新活动信息
        activityMapper.updateById(activity);
    }

    /**
     * 根据ID删除活动
     * @param id 活动ID，用于指定要删除的活动
     */
    @Override
    public void delete(Long id) {
        // 调用活动映射器的deleteById方法，根据传入的ID删除对应的活动
        activityMapper.deleteById(id);
    }

    /**
     * 查询当前时间范围内的活动列表
     * @return 当前有效的活动列表，包含活动SKU信息
     */
    @Override
    public List<Activity> selectActive() {
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 查询当前时间范围内的活动，状态不为2，并按结束时间升序排列
        List<Activity> activities = activityMapper.selectList(Wrappers.<Activity>lambdaQuery()
                .le(Activity::getStartTime, now).ge(Activity::getEndTime, now)
                .ne(Activity::getStatus, 2).orderByAsc(Activity::getEndTime));
        if (activities.isEmpty()) return activities;
        List<ActivitySku> activitySkus = activitySkuMapper.selectList(
                Wrappers.<ActivitySku>lambdaQuery().in(ActivitySku::getActivityId,
                        activities.stream().map(Activity::getId).toList()));
        Map<Long, Long> spuIds = activitySkus.isEmpty() ? Map.of() : skuMapper.selectBatchIds(
                        activitySkus.stream().map(ActivitySku::getSkuId).toList())
                .stream().collect(Collectors.toMap(Sku::getId, Sku::getSpuId));
        activitySkus.forEach(item -> item.setSpuId(spuIds.get(item.getSkuId())));
        Map<Long, List<ActivitySku>> items = activitySkus.stream().collect(Collectors.groupingBy(ActivitySku::getActivityId));
        activities.forEach(activity -> activity.setSkuItems(items.getOrDefault(activity.getId(), List.of())));
        return activities;
    }

    /**
     * 根据活动ID查询该活动下的所有SKU列表
     * @param activityId 活动ID
     * @return 返回活动SKU列表，按ID升序排列
     */
    @Override
    public List<ActivitySku> listSkus(Long activityId) {
        // 使用Lambda查询构造器查询符合条件的活动SKU
        // 查询条件为活动ID等于传入的activityId
        // 查询结果按ID升序排列
        return activitySkuMapper.selectList(Wrappers.<ActivitySku>lambdaQuery()
                .eq(ActivitySku::getActivityId, activityId).orderByAsc(ActivitySku::getId));
    }

    /** 秒杀 SKU 必须配置活动价和独立库存，限购规则默认限制每人一件。 */
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

    /** 通过条件更新原子预留活动库存，避免并发下单超过秒杀可售量。 */
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
