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
/**
 * 分类服务实现类
 * 使用@Service注解标记为Spring服务组件
 * 使用@RequiredArgsConstructor注解自动生成构造函数，注入必要的依赖
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    // 注入分类数据访问层
    private final CategoryMapper categoryMapper;

    /**
     * 查询所有分类，以树形结构返回
     * @return 分类列表
     */
    @Override
    public List<Category> selectTree() {
        return categoryMapper.selectAll();
    }

    /**
     * 根据ID查询分类
     * @param id 分类ID
     * @return 分类对象
     */
    @Override
    public Category getById(Long id) {
        return categoryMapper.selectById(id);
    }

    /**
     * 保存分类
     * @param category 分类对象
     */
    @Override
    public void save(Category category) {
        categoryMapper.insert(category);
    }

    /**
     * 更新分类信息
     * @param category 分类对象
     */
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
