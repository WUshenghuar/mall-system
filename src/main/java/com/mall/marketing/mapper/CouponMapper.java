package com.mall.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.marketing.entity.Coupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券Mapper接口
 * 继承BaseMapper，提供基础的CRUD操作
 */
public interface CouponMapper extends BaseMapper<Coupon> {
    /**
     * 根据ID查询优惠券信息（使用行级锁）
     * @param id 优惠券ID
     * @return 优惠券实体对象
     */
    @Select("SELECT * FROM mk_coupon WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Coupon selectForUpdate(@Param("id") Long id);

    @Update("UPDATE mk_coupon SET issued_count = issued_count + 1, update_time = NOW() "
            + "WHERE id = #{id} AND (max_issue IS NULL OR max_issue <= 0 OR issued_count < max_issue)")
    int increaseIssuedCountIfAvailable(@Param("id") Long id);
}
