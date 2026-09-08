package com.mall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.json.JsonData;
import com.mall.common.exception.BusinessException;
import com.mall.product.entity.Brand;
import com.mall.product.entity.Sku;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.BrandMapper;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchClient.class)
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ElasticsearchClient esClient;
    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final BrandMapper brandMapper;
    private static final String INDEX_NAME = "mall_product";
    private static final int BULK_SIZE = 500;

    @Override
    public void createIndex() {
        try {
            boolean exists = esClient.indices().exists(
                    r -> r.index(INDEX_NAME)).value();
            if (exists) {
                log.info("Index {} already exists", INDEX_NAME);
                return;
            }
            CreateIndexResponse response = esClient.indices().create(
                    r -> r.index(INDEX_NAME)
                            .mappings(m -> m
                                    .properties("spuId", p -> p.long_(l -> l))
                                    .properties("spuName", p -> p.text(t -> t.analyzer("ik_smart")))
                                    .properties("categoryId", p -> p.long_(l -> l))
                                    .properties("categoryPath", p -> p.keyword(k -> k))
                                    .properties("brand", p -> p.keyword(k -> k))
                                    .properties("minPrice", p -> p.double_(d -> d))
                                    .properties("currency", p -> p.keyword(k -> k))
                                    .properties("salesCount", p -> p.long_(l -> l))
                                    .properties("rating", p -> p.double_(d -> d))
                                    .properties("status", p -> p.byte_(b -> b))
                            )
            );
            log.info("Index created: {}", response.acknowledged());
        } catch (IOException e) {
            log.error("Failed to create index", e);
            throw new BusinessException("ES索引创建失败: " + e.getMessage());
        }
    }

    @Override
    public void indexProduct(Map<String, Object> product) {
        try {
            esClient.index(r -> r
                    .index(INDEX_NAME)
                    .id(String.valueOf(product.get("spuId")))
                    .document(product));
        } catch (IOException e) {
            log.error("Failed to index product: {}", product.get("spuId"), e);
        }
    }

    @Override
    public void indexProduct(Long spuId) {
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null) {
            deleteProduct(spuId);
            return;
        }
        List<Sku> skus = skuMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<Sku>lambdaQuery()
                        .eq(Sku::getSpuId, spuId));
        Map<Long, String> brandNames = loadBrandNames(List.of(spu));
        indexProduct(buildProductDocument(spu, skus, brandNames));
    }

    @Override
    public void deleteProduct(Long spuId) {
        try {
            esClient.delete(r -> r.index(INDEX_NAME).id(String.valueOf(spuId)));
        } catch (IOException e) {
            log.error("Failed to delete product from index: {}", spuId, e);
        }
    }

    @Override
    public Map<String, Object> search(String keyword, Long categoryId,
                                       Double minPrice, Double maxPrice,
                                       String sortField, String sortOrder,
                                       int page, int size) {
        try {
            int from = (page - 1) * size;
            SortOrder order = "desc".equalsIgnoreCase(sortOrder) ? SortOrder.Desc : SortOrder.Asc;

            SearchResponse<Map> response = esClient.search(s -> s
                            .index(INDEX_NAME)
                            .from(from)
                            .size(size)
                            .query(q -> q
                                    .bool(b -> {
                                        if (keyword != null && !keyword.isEmpty()) {
                                            b.must(m -> m.match(t -> t.field("spuName").query(keyword)));
                                        }
                                        if (categoryId != null) {
                                            b.filter(f -> f.term(t -> t.field("categoryId").value(categoryId)));
                                        }
                                        if (minPrice != null || maxPrice != null) {
                                            b.filter(f -> f.range(r -> {
                                                if (minPrice != null) {
                                                    r.gte(JsonData.of(minPrice));
                                                }
                                                if (maxPrice != null) {
                                                    r.lte(JsonData.of(maxPrice));
                                                }
                                                return r.field("minPrice");
                                            }));
                                        }
                                        return b;
                                    }))
                            .sort(s0 -> s0.field(f -> {
                                if (sortField != null) {
                                    f.field(sortField).order(order);
                                } else {
                                    f.field("salesCount").order(SortOrder.Desc);
                                }
                                return f;
                            })),
                    Map.class);

            List<Map> records = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("total", response.hits().total().value());
            result.put("page", page);
            result.put("size", size);
            return result;
        } catch (IOException e) {
            log.error("Search failed", e);
            throw new BusinessException("搜索失败: " + e.getMessage());
        }
    }

    @Override
    public void syncAllProducts() {
        createIndex();
        List<Spu> spus = spuMapper.selectList(null);
        Map<Long, List<Sku>> skusBySpuId = loadSkusBySpuId(spus);
        Map<Long, String> brandNames = loadBrandNames(spus);
        List<Map<String, Object>> documents = spus.stream()
                .map(spu -> buildProductDocument(spu,
                        skusBySpuId.getOrDefault(spu.getId(), List.of()), brandNames))
                .toList();

        for (int from = 0; from < documents.size(); from += BULK_SIZE) {
            bulkIndex(documents.subList(from, Math.min(from + BULK_SIZE, documents.size())));
        }
        log.info("Full product sync to ES completed, total={}", documents.size());
    }

    Map<String, Object> buildProductDocument(Spu spu, List<Sku> skus,
                                             Map<Long, String> brandNames) {
        Map<String, Object> document = new HashMap<>();
        document.put("spuId", spu.getId());
        document.put("spuName", spu.getSpuName());
        document.put("categoryId", spu.getCategoryId());
        document.put("brand", brandNames.get(spu.getBrandId()));
        document.put("salesCount", Optional.ofNullable(spu.getSalesCount()).orElse(0));
        document.put("status", spu.getStatus());

        skus.stream()
                .filter(sku -> sku.getPrice() != null)
                .min(Comparator.comparing(Sku::getPrice))
                .ifPresent(sku -> {
                    document.put("minPrice", sku.getPrice().doubleValue());
                    document.put("currency", sku.getCurrency());
                });
        return document;
    }

    private Map<Long, List<Sku>> loadSkusBySpuId(List<Spu> spus) {
        if (spus.isEmpty()) {
            return Map.of();
        }
        List<Long> spuIds = spus.stream().map(Spu::getId).toList();
        return skuMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.<Sku>lambdaQuery()
                        .in(Sku::getSpuId, spuIds))
                .stream()
                .collect(Collectors.groupingBy(Sku::getSpuId));
    }

    private Map<Long, String> loadBrandNames(List<Spu> spus) {
        List<Long> brandIds = spus.stream()
                .map(Spu::getBrandId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (brandIds.isEmpty()) {
            return Map.of();
        }
        return brandMapper.selectBatchIds(brandIds).stream()
                .collect(Collectors.toMap(Brand::getId, Brand::getBrandName));
    }

    void bulkIndex(List<Map<String, Object>> documents) {
        if (documents.isEmpty()) {
            return;
        }
        BulkRequest.Builder request = new BulkRequest.Builder();
        documents.forEach(document -> request.operations(operation -> operation.index(index -> index
                .index(INDEX_NAME)
                .id(String.valueOf(document.get("spuId")))
                .document(document))));
        try {
            BulkResponse response = esClient.bulk(request.build());
            if (response.errors()) {
                List<String> failedIds = new ArrayList<>();
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        failedIds.add(item.id());
                        log.error("ES bulk index failed, spuId={}, reason={}",
                                item.id(), item.error().reason());
                    }
                }
                throw new BusinessException("ES商品索引批量写入失败: " + String.join(",", failedIds));
            }
        } catch (IOException e) {
            log.error("ES bulk index request failed", e);
            throw new BusinessException("ES商品索引批量写入失败: " + e.getMessage());
        }
    }
}
