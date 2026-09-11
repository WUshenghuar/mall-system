package com.mall.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.marketing.entity.ActivitySku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface ActivitySkuMapper extends BaseMapper<ActivitySku> {
    @Select("SELECT s.* FROM mk_activity_sku s INNER JOIN mk_activity a ON a.id = s.activity_id "
            + "WHERE s.sku_id = #{skuId} AND a.start_time <= #{now} AND a.end_time >= #{now} "
            + "AND a.status <> 2 AND a.deleted = 0 ORDER BY a.end_time ASC LIMIT 1")
    ActivitySku findActiveBySkuId(@Param("skuId") Long skuId, @Param("now") LocalDateTime now);

    @Update("UPDATE mk_activity_sku SET seckill_stock = seckill_stock - #{quantity}, update_time = NOW() "
            + "WHERE activity_id = #{activityId} AND sku_id = #{skuId} "
            + "AND seckill_stock IS NOT NULL AND seckill_stock >= #{quantity}")
    int reserveStock(@Param("activityId") Long activityId, @Param("skuId") Long skuId,
                     @Param("quantity") int quantity);

    @Update("UPDATE mk_activity_sku SET seckill_stock = seckill_stock + #{quantity}, update_time = NOW() "
            + "WHERE activity_id = #{activityId} AND sku_id = #{skuId} AND seckill_stock IS NOT NULL")
    int releaseStock(@Param("activityId") Long activityId, @Param("skuId") Long skuId,
                     @Param("quantity") int quantity);
}
