package com.mall.trade.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 将结算预览固化为短时、单次使用且绑定会员的下单快照。 */
/**
 * 结算快照服务类，用于处理订单创建过程中的数据快照和校验
 * 使用Redis存储快照数据，确保订单创建时的数据一致性
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementSnapshotService {
    // Redis键前缀，用于标识结算快照数据
    private static final String KEY_PREFIX = "trade:settlement:snapshot:";
    // 快照数据在Redis中的存活时间，15分钟
    private static final Duration TTL = Duration.ofMinutes(15);

    // 结算服务，用于处理订单相关的业务逻辑
    private final SettlementService settlementService;
    // Redis操作模板，用于存储和获取快照数据
    private final StringRedisTemplate stringRedisTemplate;
    // 对象映射器，用于Java对象与JSON之间的转换
    private final ObjectMapper objectMapper;

    /** 保存结算时的商品、地址和优惠券数据，订单创建时不再信任客户端重传内容。 */
    public Map<String, Object> create(Long userId, List<Long> cartIds, Long addressId, Long couponId) {
        Map<String, Object> preview = settlementService.preview(userId, cartIds, addressId, couponId);
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            List<Map<String, Object>> previewItems = objectMapper.convertValue(preview.get("items"), new TypeReference<>() {});
            List<Map<String, Object>> orderItems = previewItems.stream()
                    .map(item -> Map.of("skuId", item.get("skuId"), "quantity", item.get("quantity"))).toList();
            String snapshot = objectMapper.writeValueAsString(new Snapshot(userId, addressId, couponId,
                    objectMapper.writeValueAsString(orderItems)));
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + token, snapshot, TTL);
        } catch (Exception e) {
            log.error("Failed to create settlement snapshot", e);
            throw new BusinessException("结算快照生成失败");
        }
        Map<String, Object> result = new HashMap<>(preview);
        result.put("snapshotToken", token);
        return result;
    }

    /** 原子取走快照，校验会员归属后返回；同一令牌无法重复创建订单。 */
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

    public record Snapshot(Long userId, Long addressId, Long couponId, String itemsJson) {}
}
