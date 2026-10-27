package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.exception.BusinessException;
import com.mall.product.entity.Category;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 管理商品分类树及分类基础信息。 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;

    @Override
    public List<Category> selectTree() {
        return categoryMapper.selectAll();
    }

    @Override
    public Category getById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public void save(Category category) {
        categoryMapper.insert(category);
    }

    @Override
    public void update(Category category) {
        categoryMapper.updateById(category);
    }

    /** 分类仍有子分类时拒绝删除，避免留下无法归属的树节点。 */
    @Override
    public void delete(Long id) {
        Long count = categoryMapper.selectCount(
                Wrappers.<Category>lambdaQuery().eq(Category::getParentId, id));
        if (count > 0) {
            throw new BusinessException("请先删除子分类");
        }
        categoryMapper.deleteById(id);
    }
}
