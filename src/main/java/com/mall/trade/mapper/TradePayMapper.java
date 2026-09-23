package com.mall.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.trade.entity.TradePay;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface TradePayMapper extends BaseMapper<TradePay> {
    @Update("UPDATE trade_pay SET pay_status = 1, pay_time = NOW(), callback_time = NOW(), "
            + "callback_content = #{content}, update_time = NOW() WHERE id = #{id} AND pay_status = 0")
    int markSuccess(@Param("id") Long id, @Param("content") String content);

    @Select("SELECT * FROM trade_pay WHERE id = #{id} FOR UPDATE")
    TradePay selectByIdForUpdate(@Param("id") Long id);
}
