package com.mall.search.service;

import java.util.Map;

public interface ProductSearchService {
    void createIndex();
    void indexProduct(Map<String, Object> product);
    void deleteProduct(Long spuId);
    Map<String, Object> search(String keyword, Long categoryId,
                                Double minPrice, Double maxPrice,
                                String sortField, String sortOrder,
                                int page, int size);
    void syncAllProducts();
}
