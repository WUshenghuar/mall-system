package com.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BusinessException;
import com.mall.finance.entity.TaxConfig;
import com.mall.finance.mapper.TaxConfigMapper;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mq.OrderMessageSender;
import com.mall.order.service.OrderService;
import com.mall.product.entity.Sku;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SpuMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SkuMapper skuMapper;
    private final SpuMapper spuMapper;
    private final TaxConfigMapper taxConfigMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private OrderMessageSender messageSender;

    public OrderServiceImpl(OrderMapper orderMapper,
                             OrderItemMapper orderItemMapper,
                             SkuMapper skuMapper,
                             SpuMapper spuMapper,
                             TaxConfigMapper taxConfigMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.skuMapper = skuMapper;
        this.spuMapper = spuMapper;
        this.taxConfigMapper = taxConfigMapper;
    }

    private static final String STOCK_KEY = "stock:sku:";
    private static final String LOCK_KEY = "lock:order:create:";

    @Override
    public String createOrder(Long userId, Long skuId, Integer quantity, Long couponId) {
        // 生成订单号
        String orderNo = "ORD" + System.currentTimeMillis()
                + String.format("%04d", (int) (Math.random() * 10000));

        // Redis 可用时：分布式锁 + 预扣库存
        if (redisTemplate != null) {
            String lockKey = LOCK_KEY + skuId + ":" + userId;
            String lockValue = UUID.randomUUID().toString();
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, 10, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(locked)) {
                throw new BusinessException("操作太频繁，请稍后重试");
            }
            try {
                Long stock = redisTemplate.opsForValue()
                        .decrement(STOCK_KEY + skuId);
                if (stock == null || stock < 0) {
                    redisTemplate.opsForValue().increment(STOCK_KEY + skuId);
                    throw new BusinessException("库存不足");
                }
                sendCreateMessage(userId, skuId, quantity, couponId, orderNo);
            } finally {
                String val = (String) redisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(val)) {
                    redisTemplate.delete(lockKey);
                }
            }
        } else {
            // 无 Redis：直接同步创建
            sendCreateMessage(userId, skuId, quantity, couponId, orderNo);
        }
        return orderNo;
    }

    private void sendCreateMessage(Long userId, Long skuId, Integer quantity,
                                    Long couponId, String orderNo) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("userId", userId);
        msg.put("skuId", skuId);
        msg.put("quantity", quantity);
        msg.put("couponId", couponId);
        msg.put("orderNo", orderNo);

        if (messageSender != null) {
            messageSender.sendOrderCreate(msg);
        } else {
            processOrderMessage(msg);
        }
    }

    @Override
    @Transactional
    public void processOrderMessage(Map<String, Object> msg) {
        String orderNo = (String) msg.get("orderNo");
        Long userId = Long.valueOf(msg.get("userId").toString());
        Long skuId = Long.valueOf(msg.get("skuId").toString());
        Integer quantity = Integer.valueOf(msg.get("quantity").toString());

        // 查询 SKU 获取真实价格
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException("SKU不存在");
        }
        BigDecimal unitPrice = sku.getPrice();
        String currency = sku.getCurrency() != null ? sku.getCurrency() : "USD";

        // 查询 SPU 获取分类（用于关税查询）
        Long categoryId = null;
        if (sku.getSpuId() != null) {
            Spu spu = spuMapper.selectById(sku.getSpuId());
            if (spu != null) {
                categoryId = spu.getCategoryId();
            }
        }

        // 查询关税税率
        BigDecimal tariffRate = getTariffRate(categoryId);
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal tariffAmount = totalAmount.multiply(tariffRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // 根据重量估算运费（默认 10 USD/kg）
        BigDecimal weight = sku.getWeight() != null ? sku.getWeight() : BigDecimal.ONE;
        BigDecimal shippingFee = weight.multiply(new BigDecimal("10"))
                .multiply(BigDecimal.valueOf(quantity));

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setCurrency(currency);
        // 汇率暂时使用默认值，后续对接汇率服务
        order.setExchangeRate(new BigDecimal("7.24"));
        order.setTariffAmount(tariffAmount);
        order.setTariffRate(tariffRate);
        order.setShippingFee(shippingFee);
        order.setPayAmount(totalAmount.add(tariffAmount).add(shippingFee));
        order.setOrderStatus(0);
        order.setPayStatus(0);
        order.setLogisticsStatus(0);
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        item.setPrice(unitPrice);
        item.setTotalPrice(totalAmount);
        orderItemMapper.insert(item);

        log.info("Order created: {} | SKU={} qty={} price={} total={} tariff={}",
                orderNo, skuId, quantity, unitPrice, totalAmount, tariffAmount);
    }

    /**
     * 查询商品分类对应的关税税率
     */
    private BigDecimal getTariffRate(Long categoryId) {
        if (taxConfigMapper == null || categoryId == null) {
            return BigDecimal.ZERO;
        }
        TaxConfig config = taxConfigMapper.selectOne(
                Wrappers.<TaxConfig>lambdaQuery()
                        .eq(TaxConfig::getCategoryId, categoryId)
                        .orderByDesc(TaxConfig::getEffectiveDate)
                        .last("LIMIT 1"));
        return config != null && config.getTaxRate() != null
                ? config.getTaxRate() : BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public void paySuccess(String orderNo) {
        Order order = orderMapper.selectOne(
                Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
        if (order == null || order.getOrderStatus() != 0) return;

        order.setOrderStatus(1);
        order.setPayStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("Order paid: {}", orderNo);
    }

    @Override
    public IPage<Order> selectPage(Integer page, Integer size,
                                    Integer orderStatus, String keyword) {
        LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery()
                .eq(orderStatus != null, Order::getOrderStatus, orderStatus)
                .like(StringUtils.hasText(keyword), Order::getOrderNo, keyword)
                .orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Order getById(Long id) {
        return orderMapper.selectById(id);
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        return orderMapper.selectOne(
                Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
    }

    @Override
    public void cancelOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        order.setOrderStatus(5);
        orderMapper.updateById(order);
    }
}
