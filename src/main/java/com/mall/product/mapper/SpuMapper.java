package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.product.entity.Spu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

public interface SpuMapper extends BaseMapper<Spu> {
    @Select({
            "<script>",
            "SELECT s.* FROM pm_spu s WHERE s.deleted = 0 AND s.status = 1",
            "<if test=\"categoryId != null\"> AND s.category_id = #{categoryId}</if>",
            "<if test=\"keyword != null and keyword != ''\"> AND s.spu_name LIKE CONCAT('%', #{keyword}, '%')</if>",
            "<if test=\"minPrice != null\"> AND (SELECT MIN(k_min.price) FROM pm_sku k_min WHERE k_min.spu_id = s.id AND k_min.deleted = 0 AND k_min.status = 1) &gt;= #{minPrice}</if>",
            "<if test=\"maxPrice != null\"> AND (SELECT MIN(k_max.price) FROM pm_sku k_max WHERE k_max.spu_id = s.id AND k_max.deleted = 0 AND k_max.status = 1) &lt;= #{maxPrice}</if>",
            "<choose>",
            "<when test=\"sortField == 'priceAsc'\"> ORDER BY (SELECT MIN(k_price.price) FROM pm_sku k_price WHERE k_price.spu_id = s.id AND k_price.deleted = 0 AND k_price.status = 1) ASC, s.create_time DESC</when>",
            "<when test=\"sortField == 'priceDesc'\"> ORDER BY (SELECT MIN(k_price.price) FROM pm_sku k_price WHERE k_price.spu_id = s.id AND k_price.deleted = 0 AND k_price.status = 1) DESC, s.create_time DESC</when>",
            "<when test=\"sortField == 'newest'\"> ORDER BY s.create_time DESC</when>",
            "<otherwise> ORDER BY s.sales_count DESC, s.create_time DESC</otherwise>",
            "</choose>",
            "</script>"
    })
    IPage<Spu> selectStorePage(IPage<Spu> page,
                                @Param("categoryId") Long categoryId,
                                @Param("keyword") String keyword,
                                @Param("minPrice") BigDecimal minPrice,
                                @Param("maxPrice") BigDecimal maxPrice,
                                @Param("sortField") String sortField);
}
