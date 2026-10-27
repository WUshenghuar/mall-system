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
import org.springframework.dao.DuplicateKeyException;
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

/** 管理订单创建与状态更新，并在消息组件不可用时保留同步创建路径。 */
/**
 * 订单服务实现类
 * 提供订单创建、支付、查询、取消等功能，支持分布式锁和消息队列
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    // 注入订单相关数据访问层
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SkuMapper skuMapper;
    private final SpuMapper spuMapper;
    private final TaxConfigMapper taxConfigMapper;

    // 可选注入Redis模板和消息发送器
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private OrderMessageSender messageSender;

    /**
     * 构造函数，注入所有必要的Mapper
     */
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



    // Redis键常量定义
    private static final String STOCK_KEY = "stock:sku:"; // 库存键前缀
    private static final String LOCK_KEY = "lock:order:create:"; // 分布式锁键前缀

    /** 接收订单创建请求；启用 Redis 时先串行化同一用户对同一 SKU 的请求并预扣库存。 */
    @Override
    public String createOrder(Long userId, Long skuId, Integer quantity, Long couponId) {
        // 生成订单号
        String orderNo = "ORD" + UUID.randomUUID().toString().replace("-", "").substring(0, 29);

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
                Long stock = redisTemplate.opsForValue().increment(STOCK_KEY + skuId, -quantity.longValue());
                if (stock == null || stock < 0) {
                    redisTemplate.opsForValue().increment(STOCK_KEY + skuId, quantity.longValue());
                    throw new BusinessException("库存不足");
                }
                try {
                    sendCreateMessage(userId, skuId, quantity, couponId, orderNo);
                } catch (RuntimeException e) {
                    redisTemplate.opsForValue().increment(STOCK_KEY + skuId, quantity.longValue());
                    throw e;
                }
            } finally {
                // 比对锁值可降低误删风险，但读取与删除非原子，过期重建时仍可能误删。
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

    /** 消费订单创建消息并落库；订单号的唯一约束让重复投递最终收敛为同一张订单。 */
    @Override
    @Transactional
    public void processOrderMessage(Map<String, Object> msg) {
        String orderNo = (String) msg.get("orderNo");
        if (orderMapper.selectCount(Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo)) > 0) return;
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
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 并发消费者可能同时通过前置查询，由数据库唯一键兜底去重。
            return;
        }

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
    /**
     * 根据分类ID获取关税税率
     * @param categoryId 分类ID
     * @return 返回对应的关税税率，如果查询失败或参数无效则返回0
     */
    private BigDecimal getTariffRate(Long categoryId) {
        // 检查mapper是否初始化或categoryId是否为null
        if (taxConfigMapper == null || categoryId == null) {
            return BigDecimal.ZERO;
        }
        // 查询最新的关税配置信息
        TaxConfig config = taxConfigMapper.selectOne(
                Wrappers.<TaxConfig>lambdaQuery()
                        // 按分类ID查询
                        .eq(TaxConfig::getCategoryId, categoryId)
                        // 按生效日期降序排列
                        .orderByDesc(TaxConfig::getEffectiveDate)
                        // 限制只返回1条记录
                        .last("LIMIT 1"));
        // 如果配置存在且税率不为null则返回税率，否则返回0
        return config != null && config.getTaxRate() != null
                ? config.getTaxRate() : BigDecimal.ZERO;
    }

    /** 将待支付订单置为已支付；重复回调或非待支付订单不重复推进状态。 */
    /**
     * 处理支付成功的订单
     * @param orderNo 订单编号
     * @Transactional 添加事务注解，确保方法内的数据库操作是一个事务
     */
    @Override
    @Transactional
    public void paySuccess(String orderNo) {
        // 根据订单编号查询订单信息
        Order order = orderMapper.selectOne(
                Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
        // 如果订单不存在或订单状态不是待支付(0)，则直接返回
        if (order == null || order.getOrderStatus() != 0) return;

        // 更新订单状态为已支付(1)
        order.setOrderStatus(1);
        // 更新支付状态为已支付(1)
        order.setPayStatus(1);
        // 设置支付时间为当前时间
        order.setPayTime(LocalDateTime.now());
        // 更新订单信息到数据库
        orderMapper.updateById(order);
        // 记录订单支付成功的日志
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

    /**
     * 根据订单ID获取订单信息
     * @param id 订单ID
     * @return 返回对应的订单对象
     */
    @Override
    public Order getById(Long id) {
        // 调用orderMapper的selectById方法查询数据库获取订单信息
        return orderMapper.selectById(id);
    }

    /**
     * 根据订单号查询订单信息
     * @param orderNo 订单号
     * @return 返回查询到的订单对象，如果不存在则返回null
     */
    @Override
    public Order getByOrderNo(String orderNo) {
        // 使用MyBatis-Plus的lambda查询构造器
        // 创建一个Order表的查询条件
        // 设置查询条件为订单号等于传入的orderNo参数
        // 执行查询并返回结果
        return orderMapper.selectOne(
                Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
    }

    /**
     * 取消订单方法
     * @param id 订单ID
     * @throws BusinessException 当订单不存在时抛出业务异常
     */
    @Override
    public void cancelOrder(Long id) {
        // 根据ID查询订单
        Order order = orderMapper.selectById(id);
        // 如果订单不存在，抛出业务异常
        if (order == null) throw new BusinessException("订单不存在");
        // 设置订单状态为已取消（状态码5）
        order.setOrderStatus(5);
        // 更新订单信息
        orderMapper.updateById(order);
    }
}
