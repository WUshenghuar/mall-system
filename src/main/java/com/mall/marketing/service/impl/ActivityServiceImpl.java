package com.mall.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.marketing.entity.Activity;
import com.mall.marketing.mapper.ActivityMapper;
import com.mall.marketing.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {
    private final ActivityMapper activityMapper;

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
        return activityMapper.selectList(Wrappers.<Activity>lambdaQuery()
                .le(Activity::getStartTime, now).ge(Activity::getEndTime, now)
                .ne(Activity::getStatus, 2).orderByAsc(Activity::getEndTime));
    }
}
