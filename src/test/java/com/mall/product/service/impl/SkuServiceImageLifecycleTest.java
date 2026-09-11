package com.mall.product.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.service.FileService;
import com.mall.product.entity.Sku;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SkuStockMapper;
import com.mall.search.service.ProductSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkuServiceImageLifecycleTest {
    @Test
    void listResolvesStoredImagePathForBackofficePreview() {
        SkuMapper skuMapper = mock(SkuMapper.class);
        SkuStockMapper stockMapper = mock(SkuStockMapper.class);
        Sku sku = new Sku();
        sku.setId(9L);
        sku.setSpuId(3L);
        sku.setImages("[\"mall-product/cover.jpg\"]");
        when(skuMapper.selectList(any())).thenReturn(List.of(sku));
        when(stockMapper.selectList(any())).thenReturn(List.of());
        FileService fileService = mock(FileService.class);
        when(fileService.getPresignedUrl("mall-product/cover.jpg")).thenReturn("http://minio/cover.jpg");
        SkuServiceImpl service = new SkuServiceImpl(skuMapper, stockMapper, mock(ObjectProvider.class));
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(service, "fileService", fileService);

        assertThat(service.listBySpuId(3L).get(0).getImageUrl()).isEqualTo("http://minio/cover.jpg");
    }

    @Test
    void replacingImageDeletesPreviousOwnedObject() {
        SkuMapper skuMapper = mock(SkuMapper.class);
        Sku stored = new Sku();
        stored.setId(9L);
        stored.setSpuId(3L);
        stored.setImages("[\"mall-product/old.jpg\"]");
        Sku replacement = new Sku();
        replacement.setId(9L);
        replacement.setSpuId(3L);
        replacement.setImages("[\"mall-product/new.jpg\"]");
        when(skuMapper.selectById(9L)).thenReturn(stored);
        when(skuMapper.updateById(replacement)).thenReturn(1);
        FileService fileService = mock(FileService.class);
        SkuServiceImpl service = new SkuServiceImpl(skuMapper, mock(SkuStockMapper.class), mock(ObjectProvider.class));
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(service, "fileService", fileService);

        service.update(replacement);

        verify(fileService).delete("mall-product/old.jpg");
    }
}
