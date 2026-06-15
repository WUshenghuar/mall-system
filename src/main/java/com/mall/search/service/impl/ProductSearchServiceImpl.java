package com.mall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.json.JsonData;
import com.mall.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ElasticsearchClient esClient;
    private static final String INDEX_NAME = "mall_product";

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
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
    }

    @Override
    public void syncAllProducts() {
        createIndex();
        log.info("Full product sync to ES completed (placeholder)");
    }
}
