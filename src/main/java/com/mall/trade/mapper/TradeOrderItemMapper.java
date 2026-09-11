package com.mall.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.trade.entity.TradeOrderItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TradeOrderItemMapper extends BaseMapper<TradeOrderItem> {
    @Select("SELECT COALESCE(SUM(i.quantity), 0) FROM trade_order_item i "
            + "INNER JOIN trade_order o ON o.order_no = i.order_no "
            + "WHERE o.user_id = #{userId} AND i.activity_id = #{activityId} AND i.sku_id = #{skuId} "
            + "AND o.order_status NOT IN (4, 6) AND o.deleted = 0 AND i.deleted = 0")
    int countActiveUserActivitySku(@Param("userId") Long userId, @Param("activityId") Long activityId,
                                   @Param("skuId") Long skuId);
}
