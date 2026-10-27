package com.mall.product.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.service.FileService;
import com.mall.product.entity.Sku;
import com.mall.product.entity.SkuStock;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SkuStockMapper;
import com.mall.product.service.SkuService;
import com.mall.search.service.ProductSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 管理 SKU 数据、库存缓存和商品图片，并在 SKU 变更后尝试同步搜索索引。 */
/**
 * SkuService实现类，提供SKU相关的业务逻辑处理
 */
@Service
public class SkuServiceImpl implements SkuService {
    // 注入Sku数据访问层
    private final SkuMapper skuMapper;
    // 注入SKU库存数据访问层
    private final SkuStockMapper skuStockMapper;
    // 注入商品搜索服务的提供者
    private final ObjectProvider<ProductSearchService> productSearchServiceProvider;

    // 可选注入Redis模板，用于缓存操作
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    // 可选注入文件服务，用于处理图片相关操作
    @Autowired(required = false)
    private FileService fileService;
    // 可选注入对象映射器，用于JSON处理
    @Autowired(required = false)
    private ObjectMapper objectMapper;

    public SkuServiceImpl(SkuMapper skuMapper,
                           SkuStockMapper skuStockMapper,
                           ObjectProvider<ProductSearchService> productSearchServiceProvider) {
        this.skuMapper = skuMapper;
        this.skuStockMapper = skuStockMapper;
        this.productSearchServiceProvider = productSearchServiceProvider;
    }

    private static final String STOCK_KEY = "stock:sku:";

    /**
     * 根据商品SPU ID查询所有相关的SKU列表
     * @param spuId 商品SPU ID
     * @return SKU列表，包含库存和图片URL信息
     */
    @Override
    public List<Sku> listBySpuId(Long spuId) {
        // 查询指定SPU的所有SKU
        List<Sku> skus = skuMapper.selectList(
                Wrappers.<Sku>lambdaQuery().eq(Sku::getSpuId, spuId));
        // 批量填充库存
        if (!skus.isEmpty()) {
            List<Long> skuIds = skus.stream().map(Sku::getId).toList();
            List<SkuStock> stocks = skuStockMapper.selectList(
                    Wrappers.<SkuStock>lambdaQuery().in(SkuStock::getSkuId, skuIds));
            Map<Long, Integer> stockMap = stocks.stream()
                    .collect(Collectors.toMap(SkuStock::getSkuId, SkuStock::getStock, (a, b) -> a));
            skus.forEach(s -> {
                s.setStock(stockMap.getOrDefault(s.getId(), 0));
                s.setImageUrl(resolveImageUrl(s.getImages()));
            });
        }
        return skus;
    }

    /**
     * 根据ID查询商品信息
     * @param id 商品ID
     * @return 返回对应的Sku对象，如果不存在则返回null
     */
    @Override
    public Sku getById(Long id) { // 实现通过ID获取商品信息的接口方法
        return skuMapper.selectById(id); // 调用Mapper层的方法查询数据库
    }

    /**
     * 保存SKU（Stock Keeping Unit，库存量单位）信息并重新索引
     *
     * @param sku 要保存的商品SKU对象
     */
    @Override
    public void save(Sku sku) {
        // 调用skuMapper的insert方法将SKU信息插入到数据库
        skuMapper.insert(sku);
        // 调用reindex方法，对该SKU所属的SPU（Standard Product Unit，标准产品单位）进行重新索引
        reindex(sku.getSpuId());
    }

    /** 更新 SKU 并在图片变更时清理旧文件，随后尝试更新所属 SPU 的搜索索引。 */
    @Override
    public void update(Sku sku) {
        Sku storedSku = skuMapper.selectById(sku.getId());
        skuMapper.updateById(sku);
        if (storedSku != null && sku.getImages() != null && !Objects.equals(storedSku.getImages(), sku.getImages())) {
            deleteImage(storedSku.getImages());
        }
        reindex(storedSku != null ? storedSku.getSpuId() : sku.getSpuId());
    }

    /**
     * 删除指定ID的商品SKU信息
     * @param id 要删除的商品SKU ID
     */
    @Override
    public void delete(Long id) {
        // 根据ID查询SKU信息
        Sku sku = skuMapper.selectById(id);
        // 从数据库中删除该SKU
        skuMapper.deleteById(id);
        // 如果Redis模板不为空，则从Redis中删除对应的库存缓存
        if (redisTemplate != null) {
            redisTemplate.delete(STOCK_KEY + id);
        }
        // 删除该SKU相关的图片信息
        deleteImage(sku == null ? null : sku.getImages());
        // 重新索引该SKU所属的SPU信息
        reindex(sku == null ? null : sku.getSpuId());
    }

    /**
     * 批量更新商品SKU的价格，并重新索引相关的SPU
     * @param skuIds 需要更新价格的SKU ID列表
     * @param price 新的价格
     */
    @Override
    public void batchUpdatePrice(List<Long> skuIds, BigDecimal price) {
        // 使用Set存储SPU ID，避免重复
        java.util.Set<Long> spuIds = new java.util.HashSet<>();
        // 遍历所有SKU ID
        for (Long id : skuIds) {
            // 根据ID查询SKU信息
            Sku sku = skuMapper.selectById(id);
            // 如果SKU存在，则更新价格
            if (sku != null) {
                sku.setPrice(price);
                skuMapper.updateById(sku);
                spuIds.add(sku.getSpuId());
            }
        }
        spuIds.forEach(this::reindex);
    }

    @Override
    public Integer getStock(Long skuId) {
        // Redis 可用时优先从缓存读取
        if (redisTemplate != null) {
            Object val = redisTemplate.opsForValue().get(STOCK_KEY + skuId);
            if (val != null) return Integer.parseInt(val.toString());
        }
        // 回退到数据库
        SkuStock skuStock = skuStockMapper.selectOne(
                Wrappers.<SkuStock>lambdaQuery().eq(SkuStock::getSkuId, skuId));
        return skuStock != null ? skuStock.getStock() : 0;
    }

    /**
     * 更新指定SKU的库存数量
     * @param skuId SKU ID
     * @param stock 要更新的库存数量
     */
    @Override
    public void updateStock(Long skuId, Integer stock) {
        // 如果Redis模板不为空，则更新Redis中的库存
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(STOCK_KEY + skuId, stock);
        }
        // 同步更新数据库
        // 根据SKU ID查询库存记录
        SkuStock skuStock = skuStockMapper.selectOne(
                Wrappers.<SkuStock>lambdaQuery().eq(SkuStock::getSkuId, skuId));
        // 如果存在库存记录，则更新该记录
        if (skuStock != null) {
            skuStock.setStock(stock);
            skuStockMapper.updateById(skuStock);
        } else {
            // 如果不存在库存记录，则创建新的库存记录
            SkuStock newStock = new SkuStock();
            newStock.setSkuId(skuId);
            newStock.setStock(stock);
            skuStockMapper.insert(newStock);
        }
    }

    /**
     * 重新索引商品
     * @param spuId 商品ID
     */
    private void reindex(Long spuId) {
        // 检查商品ID是否不为空
        if (spuId != null) {
            // 如果商品搜索服务可用，则执行商品索引操作
            productSearchServiceProvider.ifAvailable(service -> service.indexProduct(spuId));
        }
    }

    /**
     * 解析图片URL，如果图片不是完整的URL，则通过文件服务获取预签名URL
     * @param images 图片字符串，可能包含多个图片，使用特定分隔符分隔
     * @return 返回解析后的图片URL，可能是原始URL或预签名URL
     */
    private String resolveImageUrl(String images) {
        // 获取第一张图片
        String image = firstImage(images);
        // 如果图片为空、已经是http开头的URL、不包含"/"分隔符，或者文件服务未初始化，则直接返回原始图片
        if (image == null || image.startsWith("http") || !image.contains("/") || fileService == null) return image;
        // 通过文件服务获取图片的预签名URL
        return fileService.getPresignedUrl(image);
    }

    /**
     * 从JSON格式的图片列表中获取第一张图片的URL
     * @param images JSON格式的图片列表字符串
     * @return 第一张图片的URL，如果输入无效或列表为空则返回null
     */
    private String firstImage(String images) {
        // 检查objectMapper是否初始化以及images参数是否有效
        if (objectMapper == null || images == null || images.isBlank()) return null;
        try {
            // 将JSON字符串解析为字符串列表
            List<String> values = objectMapper.readValue(images, new TypeReference<>() {});
            // 返回列表中的第一张图片，如果列表为空则返回null
            return values.isEmpty() ? null : values.get(0);
        } catch (Exception ignored) {
            // 捕获所有异常并忽略，返回null
            return null;
        }
    }

    /**
     * 删除图片的方法
     * @param images 图片路径字符串，可能包含多个图片路径
     */
    private void deleteImage(String images) {
        // 获取第一个图片路径
        String image = firstImage(images);
        // 检查图片路径是否有效且不是网络图片，且fileService已初始化
        if (image != null && !image.startsWith("http") && image.contains("/") && fileService != null) {
            // 调用文件服务删除图片
            fileService.delete(image);
        }
    }
}
