package com.mall.product.service;

import com.mall.product.entity.Category;

import java.util.List;

public interface CategoryService {
    List<Category> selectTree();
    Category getById(Long id);
    void save(Category category);
    void update(Category category);
    void delete(Long id);
}
