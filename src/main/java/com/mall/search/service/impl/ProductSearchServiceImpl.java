package com.mall.search.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ObjectMapper objectMapper;
    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final BrandMapper brandMapper;
    @Value("${elasticsearch.hosts:localhost:9200}")
    private String hosts;
    private static final String INDEX_NAME = "mall_product";
    private static final int BULK_SIZE = 500;

    @Override
    public void createIndex() {
        try {
            if (indexExists()) {
                log.info("Index {} already exists", INDEX_NAME);
                return;
            }
            request("PUT", "/" + INDEX_NAME, indexMapping());
            log.info("Index created: {}", INDEX_NAME);
        } catch (IOException e) {
            log.error("Failed to create index", e);
            throw new BusinessException("ES索引创建失败: " + e.getMessage());
        }
    }

    @Override
    public void indexProduct(Map<String, Object> product) {
        try {
            request("PUT", "/" + INDEX_NAME + "/_doc/" + product.get("spuId") + "?refresh=true", product);
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
            request("DELETE", "/" + INDEX_NAME + "/_doc/" + spuId + "?refresh=true", null);
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
            Map<String, Object> response = request("POST", "/" + INDEX_NAME + "/_search",
                    searchRequest(keyword, categoryId, minPrice, maxPrice, sortField, sortOrder, page, size));
            Map<String, Object> hits = map(response.get("hits"));
            List<Map<String, Object>> records = list(hits.get("hits")).stream()
                    .map(hit -> map(hit.get("_source")))
                    .toList();
            Object total = hits.get("total");
            long totalValue = total instanceof Map<?, ?> totalMap
                    ? ((Number) totalMap.get("value")).longValue() : ((Number) total).longValue();
            return Map.of("records", records, "total", totalValue, "page", page, "size", size);
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
        try {
            StringBuilder body = new StringBuilder();
            for (Map<String, Object> document : documents) {
                body.append("{\"index\":{\"_index\":\"").append(INDEX_NAME)
                        .append("\",\"_id\":\"").append(document.get("spuId")).append("\"}}\n")
                        .append(objectMapper.writeValueAsString(document)).append('\n');
            }
            Map<String, Object> response = requestRaw("POST", "/_bulk?refresh=true", body.toString(),
                    "application/x-ndjson");
            if (Boolean.TRUE.equals(response.get("errors"))) {
                throw new BusinessException("ES商品索引批量写入失败");
            }
        } catch (IOException e) {
            log.error("ES bulk index request failed", e);
            throw new BusinessException("ES商品索引批量写入失败: " + e.getMessage());
        }
    }

    private Map<String, Object> indexMapping() {
        return Map.of("mappings", Map.of("properties", Map.of(
                "spuId", Map.of("type", "long"), "spuName", Map.of("type", "text", "analyzer", "ik_smart"),
                "categoryId", Map.of("type", "long"), "categoryPath", Map.of("type", "keyword"),
                "brand", Map.of("type", "keyword"), "minPrice", Map.of("type", "double"),
                "currency", Map.of("type", "keyword"), "salesCount", Map.of("type", "long"),
                "rating", Map.of("type", "double"), "status", Map.of("type", "byte"))));
    }

    private Map<String, Object> searchRequest(String keyword, Long categoryId, Double minPrice, Double maxPrice,
                                              String sortField, String sortOrder, int page, int size) {
        Map<String, Object> bool = new LinkedHashMap<>();
        List<Map<String, Object>> filters = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            bool.put("must", List.of(Map.of("match", Map.of("spuName", keyword))));
        }
        if (categoryId != null) {
            filters.add(Map.of("term", Map.of("categoryId", categoryId)));
        }
        if (minPrice != null || maxPrice != null) {
            Map<String, Object> range = new LinkedHashMap<>();
            if (minPrice != null) range.put("gte", minPrice);
            if (maxPrice != null) range.put("lte", maxPrice);
            filters.add(Map.of("range", Map.of("minPrice", range)));
        }
        if (!filters.isEmpty()) {
            bool.put("filter", filters);
        }
        String field = sortField == null ? "salesCount" : sortField;
        String order = sortField == null || "desc".equalsIgnoreCase(sortOrder) ? "desc" : "asc";
        return Map.of("from", Math.max(page - 1, 0) * size, "size", size,
                "query", Map.of("bool", bool), "sort", List.of(Map.of(field, Map.of("order", order))));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private List<Map<String, Object>> list(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().map(this::map).toList();
    }

    private boolean indexExists() throws IOException {
        HttpURLConnection connection = open("HEAD", "/" + INDEX_NAME);
        try {
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                return true;
            }
            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                return false;
            }
            throw new IOException("ES index check failed: HTTP " + status);
        } finally {
            connection.disconnect();
        }
    }

    private Map<String, Object> request(String method, String path, Object body) throws IOException {
        return requestRaw(method, path, body == null ? null : objectMapper.writeValueAsString(body), "application/json");
    }

    private Map<String, Object> requestRaw(String method, String path, String body, String contentType) throws IOException {
        HttpURLConnection connection = open(method, path);
        try {
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", contentType);
                connection.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String response = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (status >= 400) {
                throw new IOException("ES request failed: HTTP " + status + " " + response);
            }
            return response.isBlank() ? Map.of() : objectMapper.readValue(response, new TypeReference<>() {});
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection open(String method, String path) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create("http://" + hosts + path).toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5_000);
        connection.setReadTimeout(10_000);
        return connection;
    }
}
