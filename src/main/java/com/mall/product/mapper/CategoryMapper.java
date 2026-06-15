package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.entity.Category;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CategoryMapper extends BaseMapper<Category> {
    @Select("SELECT * FROM pm_category WHERE deleted = 0 ORDER BY order_num ASC")
    List<Category> selectAll();
}
