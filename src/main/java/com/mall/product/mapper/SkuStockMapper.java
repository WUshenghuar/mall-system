package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.entity.SkuStock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface SkuStockMapper extends BaseMapper<SkuStock> {
    @Update("UPDATE pm_sku_stock SET locked_stock = locked_stock + #{quantity} "
            + "WHERE sku_id = #{skuId} AND stock - locked_stock >= #{quantity}")
    int lockAvailableStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    @Update("UPDATE pm_sku_stock SET locked_stock = locked_stock - #{quantity} "
            + "WHERE sku_id = #{skuId} AND locked_stock >= #{quantity}")
    int releaseLockedStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    @Update("UPDATE pm_sku_stock SET stock = stock - #{quantity}, locked_stock = locked_stock - #{quantity} "
            + "WHERE sku_id = #{skuId} AND locked_stock >= #{quantity}")
    int deductLockedStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);
}
