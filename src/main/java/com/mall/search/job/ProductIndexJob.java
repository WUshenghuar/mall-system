package com.mall.search.job;

import com.mall.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 商品索引同步定时任务
 * 每天凌晨 3 点全量同步 MySQL 商品数据至 Elasticsearch
 */
/**
 * 商品索引定时任务组件
 * 使用条件注解确保仅在Elasticsearch启用时执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ProductIndexJob {

    // 注入商品搜索服务
    private final ProductSearchService searchService;

    /**
     * 定时同步商品索引到Elasticsearch
     * 每天凌晨3点执行全量同步
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void syncProductIndex() {
        // 记录开始同步的日志
        log.info("开始全量同步商品索引到 ES...");
        try {
            // 调用服务执行全量同步
            searchService.syncAllProducts();
            // 记录同步完成的日志
            log.info("商品索引同步完成");
        } catch (Exception e) {
            // 记录同步失败的日志，包含异常信息
            log.error("商品索引同步失败", e);
        }
    }
}
