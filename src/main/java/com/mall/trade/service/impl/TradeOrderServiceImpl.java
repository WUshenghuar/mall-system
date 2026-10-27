package com.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.exception.BusinessException;
import com.mall.finance.service.TaxConfigService;
import com.mall.marketing.entity.ActivitySku;
import com.mall.marketing.mapper.CouponIssueMapper;
import com.mall.marketing.service.ActivityService;
import com.mall.marketing.service.CouponService;
import com.mall.member.entity.MemberAddress;
import com.mall.member.mapper.MemberAddressMapper;
import com.mall.member.service.MemberService;
import com.mall.product.entity.Sku;
import com.mall.product.entity.SkuStock;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SkuStockMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.trade.entity.TradeCart;
import com.mall.trade.entity.TradeLogistics;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.entity.TradeOrderItem;
import com.mall.trade.mapper.TradeCartMapper;
import com.mall.trade.mapper.TradeLogisticsMapper;
import com.mall.trade.mapper.TradeOrderItemMapper;
import com.mall.trade.mapper.TradeOrderMapper;
import com.mall.trade.mq.TradeEventPublisher;
import com.mall.trade.service.TradeOrderService;
import com.mall.trade.service.RedisStockReservationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * C 端交易订单服务实现。
 *
 * <p>负责组合商品、会员地址、营销、税费、库存和订单持久化等能力；
 * 订单状态更新通过带原状态条件的 Mapper 操作完成，以降低并发请求覆盖彼此结果的风险。
 * 涉及数据库的写操作通常放在 Spring 事务中，库存 Redis 预留和业务事件则由各自组件协调。</p>
 */
@Service
@RequiredArgsConstructor
public class TradeOrderServiceImpl implements TradeOrderService {

    /** C 端订单及状态流转数据访问入口。 */
    private final TradeOrderMapper orderMapper;
    /** 订单商品明细数据访问入口。 */
    private final TradeOrderItemMapper orderItemMapper;
    /** 购物车查询与已选商品清理入口。 */
    private final TradeCartMapper cartMapper;
    /** 物流记录数据访问入口。 */
    private final TradeLogisticsMapper logisticsMapper;
    /** 商品 SKU 查询入口。 */
    private final SkuMapper skuMapper;
    /** SKU 数据库库存锁定、扣减和释放入口。 */
    private final SkuStockMapper skuStockMapper;
    /** 收货地址查询入口。 */
    private final MemberAddressMapper addressMapper;
    /** 解析下单商品及 SKU 图片 JSON 的序列化工具。 */
    private final ObjectMapper objectMapper;
    /** 发布订单创建、支付和取消等领域事件。 */
    private final TradeEventPublisher tradeEventPublisher;
    /** 维护 Redis 中的可售库存预留。 */
    private final RedisStockReservationService redisStockReservationService;
    /** 校验优惠券并计算订单优惠金额。 */
    private final CouponService couponService;
    /** 订单完成后更新会员相关数据。 */
    private final MemberService memberService;
    /** 执行优惠券领用记录的订单绑定及释放。 */
    private final CouponIssueMapper couponIssueMapper;
    /** 查询活动商品并维护活动库存。 */
    private final ActivityService activityService;
    /** 查询 SKU 对应 SPU，以确定税费分类和原产地。 */
    private final SpuMapper spuMapper;
    /** 按商品、原产地及收货地查找适用税率。 */
    private final TaxConfigService taxConfigService;

    /** 生成带 T 前缀的 32 位订单号，供接口、事件和订单明细关联使用。 */
    private String generateOrderNo() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 31);
    }

    /**
     * 创建 C 端订单。
     *
     * <p>先校验请求、已选购物车、收货地址、商品状态和结算金额，再预留 Redis/数据库/活动库存；
     * 随后写入订单及明细、核销优惠券、清理本次下单的已选购物车并发布创建事件。
     * 任一数据库步骤抛出异常时，数据库操作由事务回滚，Redis 预留由库存预留服务的事务协调机制补偿。</p>
     *
     * @param userId 当前下单会员
     * @param addressId 本次订单使用的收货地址
     * @param couponId 可选的优惠券领用记录 ID
     * @param remark 订单备注
     * @param itemsJson 下单商品 JSON 数组，每项包含 SKU ID 和购买数量
     * @return 创建完成的订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradeOrder createOrder(Long userId, Long addressId, Long couponId, String remark, String itemsJson) {
        // 1. 将接口传入的 JSON 转为内部请求对象；解析失败统一作为下单参数错误反馈。
        List<OrderItemReq> items;
        try {
            items = objectMapper.readValue(itemsJson, new TypeReference<List<OrderItemReq>>() {});
        } catch (Exception e) {
            throw new BusinessException("下单参数格式错误");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException("下单商品不能为空");
        }

        // 2. 拒绝空订单和非正购买数量，避免后续金额、库存计算接受无效数据。
        for (OrderItemReq item : items) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("商品数量不合法");
            }
        }

        // 3. 地址必须存在且归当前用户所有，不能仅凭调用方提交的地址 ID 使用他人地址。
        MemberAddress address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException("收货地址不存在");
        }
        if (!userId.equals(address.getUserId())) {
            throw new BusinessException("无权使用该收货地址");
        }

        // 4. 拒绝重复 SKU，并验证请求中的商品、数量与用户购物车中已勾选项完全一致。
        List<Long> skuIds = items.stream().map(OrderItemReq::getSkuId).distinct().toList();
        if (skuIds.size() != items.size()) throw new BusinessException("下单商品不能重复");
        validateCheckedCart(userId, items, skuIds);

        // 批量读取 SKU 和库存，避免逐个 SKU 查询造成 N+1 数据库访问。
        Map<Long, Sku> skuMap = skuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));
        Map<Long, SkuStock> stockMap = skuStockMapper.selectList(
                Wrappers.<SkuStock>lambdaQuery().in(SkuStock::getSkuId, skuIds))
                .stream().collect(Collectors.toMap(SkuStock::getSkuId, s -> s));
        // 后续预留和明细构建均按 SKU 定位数量；可售库存不包含已被其他订单锁定的数量。
        Map<Long, Integer> quantities = items.stream().collect(Collectors.toMap(OrderItemReq::getSkuId, OrderItemReq::getQuantity));
        Map<Long, Integer> availableStock = stockMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> entry.getValue().getStock() - entry.getValue().getLockedStock()));

        // 汇总商品金额和税额，并在遍历时构造可直接落库的订单明细。
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        String currency = null;
        List<TradeOrderItem> orderItems = new ArrayList<>();
        Map<Long, ActivitySku> promotions = new HashMap<>();
        for (OrderItemReq item : items) {
            Sku sku = skuMap.get(item.getSkuId());
            // 下单时再次检查商品状态和价格，避免商品下架或异常数据进入订单。
            if (sku == null || !Integer.valueOf(1).equals(sku.getStatus())) {
                throw new BusinessException("商品[" + item.getSkuId() + "]不存在或已下架");
            }
            if (sku.getPrice() == null || sku.getPrice().signum() < 0) throw new BusinessException("商品价格异常");

            // 活动商品使用活动价，并校验活动库存及每位会员的购买上限。
            ActivitySku promotion = activityService.findActiveSku(item.getSkuId());
            if (promotion != null) {
                if (promotion.getSeckillStock() != null && promotion.getSeckillStock() < item.getQuantity()) {
                    throw new BusinessException("活动库存不足");
                }
                if (promotion.getLimitPerUser() != null
                        && orderItemMapper.countActiveUserActivitySku(userId, promotion.getActivityId(), item.getSkuId())
                        + item.getQuantity() > promotion.getLimitPerUser()) {
                    throw new BusinessException("超过活动限购数量");
                }
                promotions.put(item.getSkuId(), promotion);
            }
            // 非活动商品使用 SKU 日常售价；活动价缺失时也回退到日常售价。
            BigDecimal unitPrice = promotion == null || promotion.getSeckillPrice() == null
                    ? sku.getPrice() : promotion.getSeckillPrice();
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            // 同一订单只允许单一结算币种；历史 SKU 未设置币种时按 USD 处理。
            String itemCurrency = sku.getCurrency() == null || sku.getCurrency().isBlank() ? "USD" : sku.getCurrency();
            if (currency == null) currency = itemCurrency;
            else if (!currency.equalsIgnoreCase(itemCurrency)) throw new BusinessException("暂不支持混合币种结算");

            // 根据商品分类、原产国及收货国获取税率；未匹配商品或税率时不计税。
            Spu spu = sku.getSpuId() == null ? null : spuMapper.selectById(sku.getSpuId());
            BigDecimal taxRate = spu == null ? BigDecimal.ZERO
                    : taxConfigService.findApplicableRate(spu.getCategoryId(), spu.getOriginCountry(), address.getCountry());
            if (taxRate == null) taxRate = BigDecimal.ZERO;
            taxAmount = taxAmount.add(itemTotal.multiply(taxRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));

            // 保存下单时使用的价格、数量、图片和活动信息，避免订单展示依赖商品当前数据。
            TradeOrderItem orderItem = new TradeOrderItem();
            orderItem.setSkuId(item.getSkuId());
            orderItem.setSkuName(sku.getSkuCode());
            orderItem.setSkuPrice(unitPrice);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setTotalAmount(itemTotal.setScale(2, RoundingMode.HALF_UP));
            orderItem.setSkuImage(extractFirstImage(sku.getImages()));
            orderItem.setActivityId(promotion == null ? null : promotion.getActivityId());
            orderItem.setActivityStockReserved(promotion != null && promotion.getSeckillStock() != null ? 1 : 0);
            orderItems.add(orderItem);
        }

        // 优惠券校验基于商品总额进行；没有传券时折扣为零。
        CouponService.DiscountResult coupon = couponId == null
                ? new CouponService.DiscountResult(BigDecimal.ZERO.setScale(2), null)
                : couponService.validateAndCalculateDiscount(userId, couponId, totalAmount);

        // 先原子预留 Redis 可售量，减少并发超卖；随后再通过数据库条件更新锁定库存。
        // 数据库锁定失败会抛出异常并回滚当前事务，Redis 预留的补偿由预留服务负责。
        if (!redisStockReservationService.reserveAll(quantities, availableStock)) {
            throw new BusinessException("商品库存不足，请刷新后重试");
        }
        for (OrderItemReq item : items) {
            Sku sku = skuMap.get(item.getSkuId());
            SkuStock stock = stockMap.get(item.getSkuId());
            int available = stock == null ? 0 : stock.getStock() - stock.getLockedStock();
            // 使用本次读取的库存进行提示性检查；最终并发安全由 lockAvailableStock 的条件更新保证。
            if (available < item.getQuantity()) {
                throw new BusinessException("商品[" + sku.getSkuCode() + "]库存不足，剩余" + available);
            }
            if (skuStockMapper.lockAvailableStock(item.getSkuId(), item.getQuantity()) != 1) {
                throw new BusinessException("商品[" + sku.getSkuCode() + "]库存不足，请刷新后重试");
            }
        }

        // 活动库存与普通 SKU 库存独立管理；仅对有限活动库存执行预留。
        for (OrderItemReq item : items) {
            ActivitySku promotion = promotions.get(item.getSkuId());
            if (promotion != null && promotion.getSeckillStock() != null
                    && !activityService.reserveStock(promotion, item.getQuantity())) {
                throw new BusinessException("活动库存不足，请刷新后重试");
            }
        }

        // 订单金额由商品总额减优惠、加税费构成；运费当前固定为零。
        TradeOrder order = new TradeOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setOrderStatus(0);
        order.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        order.setDiscountAmount(coupon.amount());
        order.setFreightAmount(BigDecimal.ZERO);
        order.setTaxAmount(taxAmount.setScale(2, RoundingMode.HALF_UP));
        order.setPayAmount(totalAmount.subtract(coupon.amount()).add(taxAmount).setScale(2, RoundingMode.HALF_UP));
        order.setCurrency(currency);
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(
                (address.getCountry() == null ? "" : address.getCountry()) + address.getProvince() + address.getCity()
                        + address.getDistrict() + address.getDetailAddress());
        order.setRemark(remark);
        order.setSourceType(1);
        orderMapper.insert(order);

        // 将优惠券领用记录与新订单绑定；更新行数为零说明券已失效、已被使用或不属于该用户。
        if (couponId != null && couponIssueMapper.markUsed(coupon.issueId(), couponId, userId, order.getOrderNo()) != 1) {
            throw new BusinessException("优惠券已使用或不可用");
        }

        // 先获得订单主键和订单号，再为每条商品明细补齐关联字段并逐条写入。
        for (TradeOrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            orderItem.setOrderNo(order.getOrderNo());
            orderItemMapper.insert(orderItem);
        }

        // 只清理本用户本次已勾选并下单的 SKU；未勾选商品及其他用户购物车不受影响。
        cartMapper.delete(Wrappers.lambdaQuery(TradeCart.class)
                .eq(TradeCart::getUserId, userId)
                .eq(TradeCart::getChecked, 1)
                .in(TradeCart::getSkuId, skuIds));

        // 发布创建事件供异步流程消费；事件发布器负责其投递及事务时序策略。
        tradeEventPublisher.publish("created", order.getOrderNo());
        return order;
    }

    /** 从图片 JSON 中提取首张图片；无图片或历史数据格式不合法时返回空值。 */
    private String extractFirstImage(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) return null;
        try {
            List<String> images = objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
            return images.isEmpty() ? null : images.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /** createOrder 接收的单个商品请求，仅包含 SKU 标识和购买数量。 */
    @Data
    private static class OrderItemReq {
        private Long skuId;
        private Integer quantity;
    }

    @Override
    public IPage<TradeOrder> selectPage(Integer page, Integer size, Long userId, Integer orderStatus) {
        LambdaQueryWrapper<TradeOrder> qw = Wrappers.lambdaQuery(TradeOrder.class);
        // 可选按用户和状态过滤；调用方传入空值时不添加对应条件。
        if (userId != null) qw.eq(TradeOrder::getUserId, userId);
        if (orderStatus != null) qw.eq(TradeOrder::getOrderStatus, orderStatus);
        qw.orderByDesc(TradeOrder::getCreateTime);
        return orderMapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public IPage<TradeOrder> selectAdminPage(Integer page, Integer size, Integer orderStatus, String keyword) {
        // 后台关键字按订单号模糊查询，并将每页大小限制在 1 到 100 之间。
        LambdaQueryWrapper<TradeOrder> qw = Wrappers.lambdaQuery(TradeOrder.class)
                .eq(orderStatus != null, TradeOrder::getOrderStatus, orderStatus)
                .like(StringUtils.hasText(keyword), TradeOrder::getOrderNo, keyword)
                .orderByDesc(TradeOrder::getCreateTime);
        return orderMapper.selectPage(new Page<>(page, Math.min(Math.max(size, 1), 100)), qw);
    }

    @Override
    public TradeOrder getByOrderNo(String orderNo) {
        // 订单号应唯一，selectOne 在发现重复记录时会显式暴露数据异常。
        LambdaQueryWrapper<TradeOrder> qw = Wrappers.lambdaQuery(TradeOrder.class)
                .eq(TradeOrder::getOrderNo, orderNo);
        return orderMapper.selectOne(qw);
    }

    @Override
    public TradeOrder getOwnedByOrderNo(String orderNo, Long userId) {
        TradeOrder order = getByOrderNo(orderNo);
        // 对外暴露用户订单前校验归属，避免通过猜测订单号读取他人信息。
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException("订单不存在或无权访问");
        }
        return order;
    }

    @Override
    public TradeOrder getOwnedByOrderNoForUpdate(String orderNo, Long userId) {
        // 在事务中读取并锁定当前用户订单，供后续依赖订单状态的写操作串行化使用。
        TradeOrder order = orderMapper.selectOne(Wrappers.<TradeOrder>lambdaQuery()
                .eq(TradeOrder::getOrderNo, orderNo).eq(TradeOrder::getUserId, userId).last("FOR UPDATE"));
        if (order == null) throw new BusinessException("订单不存在或无权访问");
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, Long userId) {
        TradeOrder order = getOwnedByOrderNo(orderNo, userId);
        // 仅允许从待支付状态(0)转为已取消(4)，条件更新防止与支付并发时错误取消。
        if (orderMapper.transitionOwned(orderNo, userId, 0, 4) != 1) {
            throw new BusinessException("当前状态不可取消");
        }
        // 取消后释放普通/活动库存，并恢复订单使用的优惠券。
        releaseLockedStock(orderNo);
        couponIssueMapper.releaseUsed(userId, orderNo);
        tradeEventPublisher.publish("cancelled", orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(String orderNo, Long userId) {
        TradeOrder order = getOwnedByOrderNo(orderNo, userId);
        // 仅允许已发货状态(2)转为已完成(3)，完成后记录会员订单完成数据。
        if (orderMapper.transitionOwned(orderNo, userId, 2, 3) != 1) {
            throw new BusinessException("当前状态不可确认收货");
        }
        memberService.recordOrderCompletion(userId, order.getPayAmount(), orderNo);
    }

    /** 只有订单仍处于待支付状态时才确认付款并扣减已锁库存，重复支付通知不会重复扣库存。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markPaid(String orderNo, Integer payType) {
        // Mapper 仅更新待支付订单；更新失败代表订单不存在、状态不符或通知已处理，不重复扣库存。
        if (orderMapper.markPaid(orderNo, payType) != 1) {
            return false;
        }
        // 支付成功后将已锁定数量正式扣减；任一明细异常都会使本次数据库事务回滚。
        for (TradeOrderItem item : orderItems(orderNo)) {
            if (skuStockMapper.deductLockedStock(item.getSkuId(), item.getQuantity()) != 1) {
                throw new BusinessException("订单库存状态异常，请联系管理员");
            }
        }
        tradeEventPublisher.publish("paid", orderNo);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(String orderNo, String logisticsNo, String logisticsCompany) {
        // 先进行合法状态迁移，只有可发货订单才能创建物流记录。
        if (orderMapper.markShipped(orderNo) != 1) {
            throw new BusinessException("当前状态不可发货");
        }
        TradeLogistics logistics = new TradeLogistics();
        logistics.setOrderNo(orderNo);
        logistics.setLogisticsNo(logisticsNo);
        logistics.setLogisticsCompany(logisticsCompany);
        logistics.setLogisticsStatus(1);
        logisticsMapper.insert(logistics);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreStockForRefund(String orderNo, Long userId) {
        TradeOrder order = getOwnedByOrderNo(orderNo, userId);
        // 仅退款完成(状态 6)后补回库存，防止未完成退款或重复流程提前恢复库存。
        if (!Integer.valueOf(6).equals(order.getOrderStatus())) throw new BusinessException("订单尚未完成退款");
        for (TradeOrderItem item : orderItems(orderNo)) {
            if (skuStockMapper.restoreStock(item.getSkuId(), item.getQuantity()) != 1) {
                throw new BusinessException("退款补库存失败，请联系管理员");
            }
            // 数据库库存恢复后同步释放 Redis 预留；活动商品还需释放独立的活动库存。
            redisStockReservationService.release(item.getSkuId(), item.getQuantity());
            if (Integer.valueOf(1).equals(item.getActivityStockReserved())) {
                activityService.releaseStock(item.getActivityId(), item.getSkuId(), item.getQuantity());
            }
        }
    }

    /** 定时取消过期未支付订单；条件状态更新避免与并发支付流程互相覆盖。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelExpiredOrders() {
        // 先查询超过 30 分钟仍待支付的候选订单；真正取消时仍依赖状态条件更新处理并发。
        List<TradeOrder> expired = orderMapper.selectList(Wrappers.<TradeOrder>lambdaQuery()
                .eq(TradeOrder::getOrderStatus, 0)
                .lt(TradeOrder::getCreateTime, LocalDateTime.now().minusMinutes(30)));
        int cancelled = 0;
        for (TradeOrder order : expired) {
            // 只有成功从待支付迁移到已取消的订单才释放资源并计入结果。
            if (orderMapper.transitionOwned(order.getOrderNo(), order.getUserId(), 0, 4) == 1) {
                releaseLockedStock(order.getOrderNo());
                couponIssueMapper.releaseUsed(order.getUserId(), order.getOrderNo());
                tradeEventPublisher.publish("cancelled", order.getOrderNo());
                cancelled++;
            }
        }
        return cancelled;
    }

    /** 按订单号读取全部商品明细，供支付、退款及取消等订单生命周期操作使用。 */
    private List<TradeOrderItem> orderItems(String orderNo) {
        return orderItemMapper.selectList(Wrappers.<TradeOrderItem>lambdaQuery()
                .eq(TradeOrderItem::getOrderNo, orderNo));
    }

    /**
     * 校验请求商品与当前用户已勾选购物车严格一致。
     *
     * <p>除了 SKU 集合数量一致，还要求每个 SKU 的购买数量一致，防止客户端通过直接构造请求
     * 下单未勾选商品，或绕过购物车数量更新流程。</p>
     */
    private void validateCheckedCart(Long userId, List<OrderItemReq> items, List<Long> skuIds) {
        Map<Long, Integer> requested = items.stream().collect(Collectors.toMap(OrderItemReq::getSkuId, OrderItemReq::getQuantity));
        List<TradeCart> carts = cartMapper.selectList(Wrappers.<TradeCart>lambdaQuery()
                .eq(TradeCart::getUserId, userId).eq(TradeCart::getChecked, 1).in(TradeCart::getSkuId, skuIds));
        if (carts.size() != items.size() || carts.stream().anyMatch(cart -> !requested.get(cart.getSkuId()).equals(cart.getQuantity()))) {
            throw new BusinessException("下单商品与已复核购物车不一致");
        }
    }

    /**
     * 释放订单尚未正式扣减的锁定库存。
     *
     * <p>除数据库锁定库存外，同时释放 Redis 预留；若明细标记了活动库存预留，
     * 也调用活动服务释放对应数量。数据库更新异常会中断流程并触发事务回滚。</p>
     */
    private void releaseLockedStock(String orderNo) {
        for (TradeOrderItem item : orderItems(orderNo)) {
            if (skuStockMapper.releaseLockedStock(item.getSkuId(), item.getQuantity()) != 1) {
                throw new BusinessException("订单库存状态异常，请联系管理员");
            }
            redisStockReservationService.release(item.getSkuId(), item.getQuantity());
            if (Integer.valueOf(1).equals(item.getActivityStockReserved())) {
                activityService.releaseStock(item.getActivityId(), item.getSkuId(), item.getQuantity());
            }
        }
    }
}
