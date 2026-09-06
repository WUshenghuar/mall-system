package com.mall.trade.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettlementSnapshotService {
    private static final String KEY_PREFIX = "trade:settlement:snapshot:";
    private static final Duration TTL = Duration.ofMinutes(15);

    private final SettlementService settlementService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> create(Long userId, List<Long> cartIds, Long addressId) {
        Map<String, Object> preview = settlementService.preview(userId, cartIds, addressId);
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            List<Map<String, Object>> previewItems = objectMapper.convertValue(preview.get("items"), new TypeReference<>() {});
            List<Map<String, Object>> orderItems = previewItems.stream()
                    .map(item -> Map.of("skuId", item.get("skuId"), "quantity", item.get("quantity"))).toList();
            String snapshot = objectMapper.writeValueAsString(new Snapshot(userId, addressId, objectMapper.writeValueAsString(orderItems)));
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + token, snapshot, TTL);
        } catch (Exception e) {
            throw new BusinessException("结算快照生成失败");
        }
        Map<String, Object> result = new HashMap<>(preview);
        result.put("snapshotToken", token);
        return result;
    }

    public Snapshot consume(String token, Long userId) {
        if (token == null || token.isBlank()) throw new BusinessException("结算快照不能为空");
        String value = stringRedisTemplate.opsForValue().getAndDelete(KEY_PREFIX + token);
        if (value == null) throw new BusinessException("结算快照已失效，请重新结算");
        try {
            Snapshot snapshot = objectMapper.readValue(value, Snapshot.class);
            if (!userId.equals(snapshot.userId())) throw new BusinessException("无权使用该结算快照");
            return snapshot;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("结算快照无效，请重新结算");
        }
    }

    public record Snapshot(Long userId, Long addressId, String itemsJson) {}
}
