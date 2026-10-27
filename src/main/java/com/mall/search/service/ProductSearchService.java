package com.mall.search.service;

import java.util.Map;

/**
 * 商品搜索服务接口
 * 定义了商品搜索相关的操作方法，包括索引创建、商品索引、删除商品、搜索商品和同步所有商品等功能
 */
public interface ProductSearchService {
/**
 * 创建索引的方法
 * 该方法用于创建某种数据结构的索引，可能是为了提高数据检索效率
 * 具体实现依赖于具体的数据结构和业务需求
 */
    void createIndex(); // 声明一个创建索引的方法，没有参数和返回值
    /**
     * 索引商品信息
     * @param product 包含商品信息的Map集合，键为属性名，值为属性值
     */
    void indexProduct(Map<String, Object> product);
    /**
     * 根据商品ID索引商品信息
     * @param spuId 商品ID
     */
    void indexProduct(Long spuId);
    /**
     * 删除指定商品ID的索引
     * @param spuId 要删除的商品ID
     */
    void deleteProduct(Long spuId);
    Map<String, Object> search(String keyword, Long categoryId,
                                Double minPrice, Double maxPrice,
                                String sortField, String sortOrder,
                                int page, int size);
    void syncAllProducts();
}
