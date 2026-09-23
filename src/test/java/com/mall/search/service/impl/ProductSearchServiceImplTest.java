package com.mall.search.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.product.entity.Sku;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.BrandMapper;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SpuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProductSearchServiceImplTest {

    @Test
    void shouldBuildDocumentUsingSpuAndLowestSkuPrice() {
        ProductSearchServiceImpl service = service();
        Spu spu = new Spu();
        spu.setId(1L);
        spu.setSpuName("测试商品");
        spu.setCategoryId(10L);
        spu.setBrandId(20L);
        spu.setOriginCountry("CN");
        spu.setCreateTime(LocalDateTime.of(2026, 9, 12, 10, 0));
        spu.setSalesCount(8);
        spu.setStatus(1);
        Sku expensiveSku = sku("USD", "39.90");
        Sku cheapSku = sku("USD", "29.90");

        Map<String, Object> document = service.buildProductDocument(spu,
                List.of(expensiveSku, cheapSku), Map.of(20L, "测试品牌"));

        assertThat(document).containsEntry("spuId", 1L)
                .containsEntry("spuName", "测试商品")
                .containsEntry("categoryId", 10L)
                .containsEntry("brand", "测试品牌")
                .containsEntry("originCountry", "CN")
                .containsEntry("createTime", LocalDateTime.of(2026, 9, 12, 10, 0))
                .containsEntry("minPrice", 29.90d)
                .containsEntry("currency", "USD")
                .containsEntry("salesCount", 8)
                .containsEntry("status", 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldTranslateCatalogSortAndKeepPublishedFilter() {
        Map<String, Object> request = service().searchRequest("商品", 10L,
                10d, 50d, "priceAsc", null, 2, 20);
        Map<String, Object> query = (Map<String, Object>) request.get("query");
        Map<String, Object> bool = (Map<String, Object>) query.get("bool");
        List<Map<String, Object>> filters = (List<Map<String, Object>>) bool.get("filter");
        assertThat(filters).anySatisfy(filter -> assertThat(filter.toString()).contains("status=1"));
        assertThat(request.get("sort")).asString().contains("minPrice", "asc");
    }

    @Test
    void shouldOmitPriceWhenSkusHaveNoPrice() {
        Spu spu = new Spu();
        spu.setId(1L);
        spu.setBrandId(1L);
        Map<String, Object> document = service().buildProductDocument(spu, List.of(new Sku()), Map.of());

        assertThat(document).doesNotContainKeys("minPrice", "currency");
    }

    @Test
    void shouldBoundPublicSearchPagination() {
        Map<String, Object> request = service().searchRequest(null, null,
                null, null, null, null, -1, 1_000_000);

        assertThat(request).containsEntry("from", 0).containsEntry("size", 100);
    }

    @Test
    void fullSyncRecreatesIndexSoDeletedProductsDoNotRemainSearchable() throws Exception {
        List<String> requests = new CopyOnWriteArrayList<>();
        try (ServerSocket server = new ServerSocket(0)) {
            Thread responder = Thread.startVirtualThread(() -> serveElasticsearch(server, requests));
            ProductSearchServiceImpl service = service();
            ReflectionTestUtils.setField(service, "hosts", "127.0.0.1:" + server.getLocalPort());

            service.syncAllProducts();

            assertThat(requests).containsSubsequence("DELETE /mall_product", "PUT /mall_product");
            server.close();
            responder.join(1_000);
        }
    }

    private void serveElasticsearch(ServerSocket server, List<String> requests) {
        try {
            while (!server.isClosed()) {
                try (Socket socket = server.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                     PrintWriter writer = new PrintWriter(socket.getOutputStream(), false, StandardCharsets.UTF_8)) {
                    String request = reader.readLine();
                    if (request == null) continue;
                    String[] parts = request.split(" ");
                    requests.add(parts[0] + " " + parts[1]);
                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) { }
                    byte[] body = "HEAD".equals(parts[0]) ? new byte[0] : "{}".getBytes(StandardCharsets.UTF_8);
                    writer.printf("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: %d\r\nConnection: close\r\n\r\n", body.length);
                    writer.flush();
                    socket.getOutputStream().write(body);
                    socket.getOutputStream().flush();
                }
            }
        } catch (Exception ignored) { }
    }

    private ProductSearchServiceImpl service() {
        return new ProductSearchServiceImpl(new ObjectMapper(), mock(SpuMapper.class),
                mock(SkuMapper.class), mock(BrandMapper.class));
    }

    private Sku sku(String currency, String price) {
        Sku sku = new Sku();
        sku.setCurrency(currency);
        sku.setPrice(new BigDecimal(price));
        return sku;
    }
}
