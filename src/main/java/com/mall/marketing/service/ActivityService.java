package com.mall.marketing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.marketing.entity.Activity;
import com.mall.marketing.entity.ActivitySku;

import java.util.List;

public interface ActivityService {
    IPage<Activity> selectPage(Integer page, Integer size, Integer status);
    Activity getById(Long id);
    void save(Activity activity);
    void update(Activity activity);
    void delete(Long id);
    List<Activity> selectActive();
    List<ActivitySku> listSkus(Long activityId);
    void saveSku(Long activityId, ActivitySku activitySku);
    void deleteSku(Long activityId, Long skuId);
}
