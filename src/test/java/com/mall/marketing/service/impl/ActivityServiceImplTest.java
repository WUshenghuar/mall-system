package com.mall.marketing.service.impl;

import com.mall.marketing.entity.Activity;
import com.mall.marketing.entity.ActivitySku;
import com.mall.marketing.mapper.ActivityMapper;
import com.mall.marketing.mapper.ActivitySkuMapper;
import com.mall.common.exception.BusinessException;
import com.mall.product.entity.Sku;
import com.mall.product.mapper.SkuMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        new ActivityServiceImpl(mapper, mock(ActivitySkuMapper.class), mock(SkuMapper.class)).save(activity);

        assertThat(activity.getActivityType()).isEqualTo("DISCOUNT");
        assertThat(activity.getStatus()).isZero();
        verify(mapper).insert(activity);
    }

    @Test
    void saveSkuValidatesSkuAndUpsertsAssociation() {
        ActivityMapper activityMapper = mock(ActivityMapper.class);
        ActivitySkuMapper itemMapper = mock(ActivitySkuMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        when(activityMapper.selectById(7L)).thenReturn(new Activity());
        Sku sku = new Sku(); sku.setId(9L); sku.setStatus(1);
        when(skuMapper.selectById(9L)).thenReturn(sku);
        when(itemMapper.selectOne(any())).thenReturn(null);

        ActivitySku item = new ActivitySku();
        item.setSkuId(9L); item.setSeckillStock(10);
        new ActivityServiceImpl(activityMapper, itemMapper, skuMapper).saveSku(7L, item);

        assertThat(item.getActivityId()).isEqualTo(7L);
        assertThat(item.getLimitPerUser()).isEqualTo(1);
        verify(itemMapper).insert(item);
    }

    @Test
    void activeActivitiesIncludeAssociatedSkus() {
        ActivityMapper activityMapper = mock(ActivityMapper.class);
        ActivitySkuMapper itemMapper = mock(ActivitySkuMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        Activity activity = new Activity(); activity.setId(7L);
        ActivitySku item = new ActivitySku(); item.setActivityId(7L); item.setSkuId(9L);
        when(activityMapper.selectList(any())).thenReturn(java.util.List.of(activity));
        when(itemMapper.selectList(any())).thenReturn(java.util.List.of(item));

        var result = new ActivityServiceImpl(activityMapper, itemMapper, skuMapper).selectActive();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSkuItems()).containsExactly(item);
    }

    @Test
    void rejectsActivityPriceAboveOriginalSkuPrice() {
        ActivityMapper activityMapper = mock(ActivityMapper.class);
        ActivitySkuMapper itemMapper = mock(ActivitySkuMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        Activity activity = new Activity(); activity.setActivityType("SECKILL");
        when(activityMapper.selectById(7L)).thenReturn(activity);
        Sku sku = new Sku(); sku.setId(9L); sku.setStatus(1); sku.setPrice(new java.math.BigDecimal("100"));
        when(skuMapper.selectById(9L)).thenReturn(sku);
        ActivitySku item = new ActivitySku(); item.setSkuId(9L); item.setSeckillPrice(new java.math.BigDecimal("101"));
        item.setSeckillStock(10);

        assertThatThrownBy(() -> new ActivityServiceImpl(activityMapper, itemMapper, skuMapper).saveSku(7L, item))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不能高于");
    }

    @Test
    void activeSkuQueryReceivesApplicationTime() {
        ActivityMapper activityMapper = mock(ActivityMapper.class);
        ActivitySkuMapper itemMapper = mock(ActivitySkuMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        ActivitySku item = new ActivitySku(); item.setActivityId(7L); item.setSkuId(9L);
        when(itemMapper.findActiveBySkuId(org.mockito.ArgumentMatchers.eq(9L), any())).thenReturn(item);

        ActivitySku result = new ActivityServiceImpl(activityMapper, itemMapper, skuMapper).findActiveSku(9L);

        assertThat(result).isSameAs(item);
        verify(itemMapper).findActiveBySkuId(org.mockito.ArgumentMatchers.eq(9L), any());
    }
}
