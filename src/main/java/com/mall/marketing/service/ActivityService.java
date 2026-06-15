package com.mall.marketing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.marketing.entity.Activity;

public interface ActivityService {
    IPage<Activity> selectPage(Integer page, Integer size, Integer status);
    Activity getById(Long id);
    void save(Activity activity);
    void update(Activity activity);
    void delete(Long id);
}
