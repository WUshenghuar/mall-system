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
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ProductIndexJob {

    private final ProductSearchService searchService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void syncProductIndex() {
        log.info("开始全量同步商品索引到 ES...");
        try {
            searchService.syncAllProducts();
            log.info("商品索引同步完成");
        } catch (Exception e) {
            log.error("商品索引同步失败", e);
        }
    }
}
