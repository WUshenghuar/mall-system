package com.mall.marketing.service.impl;

import com.mall.marketing.entity.Activity;
import com.mall.marketing.mapper.ActivityMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ActivityServiceImplTest {
    @Test
    void saveDefaultsRequiredActivityTypeAndDraftStatus() {
        ActivityMapper mapper = mock(ActivityMapper.class);
        doAnswer(invocation -> {
            ((Activity) invocation.getArgument(0)).setId(1L);
            return 1;
        }).when(mapper).insert(any(Activity.class));

        Activity activity = new Activity();
        activity.setActivityName("跨境折扣");
        new ActivityServiceImpl(mapper).save(activity);

        assertThat(activity.getActivityType()).isEqualTo("DISCOUNT");
        assertThat(activity.getStatus()).isZero();
        verify(mapper).insert(activity);
    }
}
