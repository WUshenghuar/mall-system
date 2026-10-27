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

/** 将商品主数据映射为 Elasticsearch 文档，并提供索引同步和商品检索。 */
/**
 * 商品搜索服务实现类
 * 使用Elasticsearch实现商品搜索功能
 * 支持索引创建、商品索引、删除和搜索功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ProductSearchServiceImpl implements ProductSearchService {



    // 注入相关Mapper
    private final ObjectMapper objectMapper;  // 用于JSON序列化和反序列化
    private final SpuMapper spuMapper;        // SPU数据访问层
    private final SkuMapper skuMapper;        // SKU数据访问层
    private final BrandMapper brandMapper;    // 品牌数据访问层
    // 从配置文件中获取Elasticsearch主机地址
    @Value("${elasticsearch.hosts:localhost:9200}")
    private String hosts;


    // 常量定义
    private static final String INDEX_NAME = "mall_product";  // Elasticsearch索引名称
    private static final int BULK_SIZE = 500;                // 批量操作大小限制

    /**
     * 创建Elasticsearch索引
     * 如果索引已存在且分析器支持，则直接返回
     * 如果索引已存在但分析器不支持，则重建索引
     */
    @Override
    public void createIndex() {
        try {
            if (indexExists()) {
                if (!usesUnsupportedAnalyzer()) {
                    log.info("Index {} already exists", INDEX_NAME);
                    return;
                }
                log.warn("Rebuilding {} because its mapping uses unavailable analyzer", INDEX_NAME);
                request("DELETE", "/" + INDEX_NAME, null);
            }
            request("PUT", "/" + INDEX_NAME, indexMapping());
            log.info("Index created: {}", INDEX_NAME);
        } catch (IOException e) {
            log.error("Failed to create index", e);
            throw new BusinessException("ES索引创建失败: " + e.getMessage());
        }
    }

    /**
     * 重建索引
     * 删除现有索引并创建新索引
     */
    private void recreateIndex() {
        try {
            // ponytail: current single index accepts a brief rebuild window; use aliases when zero-downtime is required.
            if (indexExists()) request("DELETE", "/" + INDEX_NAME, null);
            request("PUT", "/" + INDEX_NAME, indexMapping());
        } catch (IOException e) {
            throw new BusinessException("ES索引重建失败: " + e.getMessage());
        }
    }

    /**
     * 索引单个商品
     * @param product 商品数据
     */
    @Override
    public void indexProduct(Map<String, Object> product) {
        try {
            request("PUT", "/" + INDEX_NAME + "/_doc/" + product.get("spuId") + "?refresh=true", product);
        } catch (IOException e) {
            log.error("Failed to index product: {}", product.get("spuId"), e);
        }
    }

    /**
     * 根据SPU ID索引商品
     * @param spuId 商品SPU ID
     */
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

    /**
     * 从索引中删除商品
     * @param spuId 商品SPU ID
     */
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

    /** 重建索引后分批写入全量商品，限制单次 bulk 请求大小以控制请求体规模。 */
    @Override
    public void syncAllProducts() {
        recreateIndex();
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

    /** 生成 SPU 检索文档，并用带价格 SKU 中的最低价代表商品价格。 */
    Map<String, Object> buildProductDocument(Spu spu, List<Sku> skus,
                                             Map<Long, String> brandNames) {
        Map<String, Object> document = new HashMap<>();
        document.put("spuId", spu.getId());
        document.put("spuName", spu.getSpuName());
        document.put("categoryId", spu.getCategoryId());
        document.put("brand", brandNames.get(spu.getBrandId()));
        document.put("originCountry", spu.getOriginCountry());
        document.put("createTime", spu.getCreateTime());
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

    /**
     * 根据SPU ID列表加载对应的SKU列表
     * @param spus SPU对象列表
     * @return 以SPU ID为键，对应的SKU列表为值的Map
     */
    private Map<Long, List<Sku>> loadSkusBySpuId(List<Spu> spus) {
        // 如果SPU列表为空，直接返回空Map
        if (spus.isEmpty()) {
            return Map.of();
        }
        // 从SPU对象列表中提取SPU ID列表
        List<Long> spuIds = spus.stream().map(Spu::getId).toList();
        // 查询所有属于这些SPU ID的SKU，并按SPU ID分组
        return skuMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.<Sku>lambdaQuery()
                        .in(Sku::getSpuId, spuIds))
                .stream()
                .collect(Collectors.groupingBy(Sku::getSpuId));
    }

    /**
     * 根据商品列表加载品牌名称信息
     * @param spus 商品列表
     * @return 品牌ID与品牌名称的映射关系
     */
    private Map<Long, String> loadBrandNames(List<Spu> spus) {
        // 从商品列表中提取品牌ID，过滤掉null值，并去重
        List<Long> brandIds = spus.stream()
                .map(Spu::getBrandId)    // 提取每个商品的品牌ID
                .filter(Objects::nonNull) // 过滤掉null值
                .distinct()              // 去重
                .toList();               // 转换为List
        // 如果品牌ID列表为空，返回空Map
        if (brandIds.isEmpty()) {
            return Map.of();
        }
        // 查询品牌信息，并构建ID与名称的映射关系
        return brandMapper.selectBatchIds(brandIds).stream()
                .collect(Collectors.toMap(Brand::getId, Brand::getBrandName)); // 将品牌列表转换为ID到名称的Map
    }

    /**
     * 批量索引文档到Elasticsearch
     * @param documents 包含要索引的文档的列表，每个文档是一个Map，包含字段名和对应的值
     */
    void bulkIndex(List<Map<String, Object>> documents) {
        // 如果文档列表为空，直接返回
        if (documents.isEmpty()) {
            return;
        }
        try {
            // 构建批量请求的请求体
            StringBuilder body = new StringBuilder();
            // 遍历每个文档，构建批量请求的格式
            for (Map<String, Object> document : documents) {
                // 添加索引元数据，包括索引名称和文档ID
                body.append("{\"index\":{\"_index\":\"").append(INDEX_NAME)
                        .append("\",\"_id\":\"").append(document.get("spuId")).append("\"}}\n")
                        // 添加文档内容，将Map转换为JSON字符串
                        .append(objectMapper.writeValueAsString(document)).append('\n');
            }
            // 发送批量请求到Elasticsearch，refresh=true确保立即刷新索引
            Map<String, Object> response = requestRaw("POST", "/_bulk?refresh=true", body.toString(),
                    "application/x-ndjson");
            // 检查响应中是否有错误
            if (Boolean.TRUE.equals(response.get("errors"))) {
                // 如果有错误，抛出业务异常
                throw new BusinessException("ES商品索引批量写入失败");
            }
        } catch (IOException e) {
            // 记录错误日志
            log.error("ES bulk index request failed", e);
            // 抛出包含详细错误信息的业务异常
            throw new BusinessException("ES商品索引批量写入失败: " + e.getMessage());
        }
    }

    /**
     * 创建索引映射配置，定义Elasticsearch索引中各个字段的数据类型和属性
     * @return 返回一个包含索引映射配置的Map对象
     */
    private Map<String, Object> indexMapping() {
        // 使用Map.of方法创建不可变的映射配置
        // 外层Map包含"mappings"键，其值是另一个Map
        return Map.of("mappings", Map.of("properties", Map.ofEntries(
                // spuId字段：长整型，用于存储商品SPU ID
                Map.entry("spuId", Map.of("type", "long")),
                // spuName字段：文本类型，使用standard分词器进行文本分析
                Map.entry("spuName", Map.of("type", "text", "analyzer", "standard")),
                // categoryId字段：长整型，用于存储商品分类ID
                Map.entry("categoryId", Map.of("type", "long")),
                // categoryPath字段：关键字类型，用于存储商品分类路径
                Map.entry("categoryPath", Map.of("type", "keyword")),
                // brand字段：关键字类型，用于存储品牌信息
                Map.entry("brand", Map.of("type", "keyword")),
                // originCountry字段：关键字类型，用于存储原产国信息
                Map.entry("originCountry", Map.of("type", "keyword")),
                // createTime字段：日期类型，用于创建时间
                Map.entry("createTime", Map.of("type", "date")),
                // minPrice字段：双精度浮点型，用于存储最低价格
                Map.entry("minPrice", Map.of("type", "double")),
                // currency字段：关键字类型，用于存储货币类型
                Map.entry("currency", Map.of("type", "keyword")),
                // salesCount字段：长整型，用于存储销售数量
                Map.entry("salesCount", Map.of("type", "long")),
                // rating字段：双精度浮点型，用于存储商品评分
                Map.entry("rating", Map.of("type", "double")),
                // status字段：字节类型，用于存储商品状态
                Map.entry("status", Map.of("type", "byte")))));
    }

    private boolean usesUnsupportedAnalyzer() {
        try {
            return objectMapper.writeValueAsString(request("GET", "/" + INDEX_NAME + "/_mapping", null))
                    .contains("\"ik_smart\"");
        } catch (IOException e) {
            return true;
        }
    }

    /** 将分页、筛选和排序参数转换为 ES DSL，并限制页码、页大小和排序字段。 */
    Map<String, Object> searchRequest(String keyword, Long categoryId, Double minPrice, Double maxPrice,
                                      String sortField, String sortOrder, int page, int size) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        Map<String, Object> bool = new LinkedHashMap<>();
        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(Map.of("term", Map.of("status", 1)));
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
        String selectedSort = sortField == null ? "" : sortField;
        String field = switch (selectedSort) {
            case "priceAsc", "priceDesc" -> "minPrice";
            case "newest" -> "createTime";
            default -> "salesCount";
        };
        String order = "priceAsc".equals(selectedSort) ? "asc" : "desc";
        if ("asc".equalsIgnoreCase(sortOrder)) order = "asc";
        if ("desc".equalsIgnoreCase(sortOrder)) order = "desc";
        return Map.of("from", Math.max(page - 1, 0) * size, "size", size,
                "query", Map.of("bool", bool), "sort", List.of(Map.of(field, Map.of("order", order))));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    /**
     * 将输入的对象转换为包含Map的List
     * @param value 输入的对象，期望是List类型
     * @return 如果输入是List类型，则返回每个元素经过map方法转换后的List；否则返回空List
     */
    /**
     * 处理输入对象并将其转换为特定格式的列表
     * @param value 输入的对象，可以是任意类型
     * @return 返回一个Map列表，如果输入不是List类型则返回空列表
     */
    private List<Map<String, Object>> list(Object value) {
        // 使用模式匹配检查输入是否为List类型
        if (!(value instanceof List<?> values)) {
            // 如果不是List类型，返回空List
            return List.of();
        }
        // 如果是List类型，使用Stream对流进行处理：
        // 1. 将List转换为Stream
        // 2. 对每个元素应用map方法进行转换
        // 3. 将结果收集为新的List
        return values.stream().map(this::map).toList();
    }

    /**
     * 检查指定的索引是否存在
     * @return 如果索引存在返回true，不存在返回false
     * @throws IOException 如果HTTP请求失败或返回意外的状态码
     */
    private boolean indexExists() throws IOException {
        // 创建一个HEAD请求连接，用于检查索引是否存在
        HttpURLConnection connection = open("HEAD", "/" + INDEX_NAME);
        try {
            // 获取HTTP响应状态码
            int status = connection.getResponseCode();
            // 如果状态码为200 OK，表示索引存在
            if (status == HttpURLConnection.HTTP_OK) {
                return true;
            }
            // 如果状态码为404 Not Found，表示索引不存在
            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                return false;
            }
            // 如果返回其他状态码，抛出异常
            throw new IOException("ES index check failed: HTTP " + status);
        } finally {
            // 确保连接被关闭，避免资源泄漏
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
