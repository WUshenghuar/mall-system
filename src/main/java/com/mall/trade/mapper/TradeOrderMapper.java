package com.mall.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.trade.entity.TradeOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface TradeOrderMapper extends BaseMapper<TradeOrder> {
    @Select("SELECT HOUR(create_time) AS hour, COUNT(*) AS count FROM trade_order "
            + "WHERE create_time >= #{start} GROUP BY HOUR(create_time)")
    List<Map<String, Object>> countByHourSince(@Param("start") LocalDateTime start);
    @Update("UPDATE trade_order SET order_status = #{targetStatus}, update_time = NOW() "
            + "WHERE order_no = #{orderNo} AND user_id = #{userId} AND order_status = #{expectedStatus}")
    int transitionOwned(@Param("orderNo") String orderNo, @Param("userId") Long userId,
                        @Param("expectedStatus") int expectedStatus, @Param("targetStatus") int targetStatus);

    @Update("UPDATE trade_order SET order_status = 1, pay_type = #{payType}, pay_time = NOW(), update_time = NOW() "
            + "WHERE order_no = #{orderNo} AND order_status = 0")
    int markPaid(@Param("orderNo") String orderNo, @Param("payType") Integer payType);

    @Update("UPDATE trade_order SET order_status = 2, delivery_time = NOW(), update_time = NOW() "
            + "WHERE order_no = #{orderNo} AND order_status = 1")
    int markShipped(@Param("orderNo") String orderNo);
}
