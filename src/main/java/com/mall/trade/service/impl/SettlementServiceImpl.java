package com.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.exception.BusinessException;
import com.mall.finance.service.TaxConfigService;
import com.mall.marketing.entity.ActivitySku;
import com.mall.marketing.entity.MemberCouponVO;
import com.mall.marketing.service.ActivityService;
import com.mall.marketing.service.CouponService;
import com.mall.member.entity.MemberAddress;
import com.mall.member.mapper.MemberAddressMapper;
import com.mall.product.entity.Sku;
import com.mall.product.entity.Spu;
import com.mall.product.entity.SkuStock;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.product.mapper.SkuStockMapper;
import com.mall.trade.entity.TradeCart;
import com.mall.trade.mapper.TradeCartMapper;
import com.mall.trade.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 计算结算预览中的在售商品金额、税费、活动价和可用优惠券。 */
/**
 * 结算服务实现类
 * 实现了会员购物车商品结算、税费计算、优惠处理等功能
 */
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    // 注入各种数据访问层和服务层组件
    private final TradeCartMapper cartMapper;          // 购物车数据访问层
    private final SkuMapper skuMapper;                // SKU商品数据访问层
    private final SkuStockMapper stockMapper;          // SKU库存数据访问层
    private final MemberAddressMapper addressMapper;   // 会员地址数据访问层
    private final CouponService couponService;         // 优惠券服务
    private final ActivityService activityService;     // 活动服务
    private final SpuMapper spuMapper;                // SPU商品数据访问层
    private final TaxConfigService taxConfigService;  // 税费配置服务

    /** 只结算当前会员勾选的购物车项，并依据会员地址计算适用税费。 */
    @Override
    public Map<String, Object> preview(Long userId, List<Long> cartIds, Long addressId, Long couponId) {
        // 1. 校验收货地址：根据地址ID查询，若不存在或该地址不属于当前用户则抛出业务异常
        MemberAddress address = addressMapper.selectById(addressId);
        if (address == null || !userId.equals(address.getUserId())) throw new BusinessException("收货地址不存在或无权使用");

        // 2. 查询并校验购物车项：查询出属于当前用户、在指定cartIds内且已勾选的购物车记录
        List<TradeCart> carts = cartMapper.selectList(Wrappers.<TradeCart>lambdaQuery()
                .eq(TradeCart::getUserId, userId).in(TradeCart::getId, cartIds).eq(TradeCart::getChecked, 1));
        // 若查出的购物车记录数与传入的cartIds数量不一致，说明部分商品不存在或未勾选
        if (carts.size() != cartIds.size()) throw new BusinessException("结算商品不存在或未勾选");

        // 初始化结算汇总变量：商品总金额、总税费、统一币种及明细列表
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        String currency = null;
        List<Map<String, Object>> items = new ArrayList<>();

        // 3. 遍历购物车，校验商品状态并计算金额
        for (TradeCart cart : carts) {
            Sku sku = skuMapper.selectById(cart.getSkuId());
            SkuStock stock = stockMapper.selectOne(Wrappers.<SkuStock>lambdaQuery().eq(SkuStock::getSkuId, cart.getSkuId()));
            // 计算可用库存 = 总库存 - 锁定库存
            int available = stock == null ? 0 : stock.getStock() - stock.getLockedStock();
            // 校验商品状态：SKU不存在、已下架(status!=1)或可用库存不足均抛出异常
            if (sku == null || !Integer.valueOf(1).equals(sku.getStatus()) || available < cart.getQuantity()) {
                throw new BusinessException("存在已下架或库存不足的商品");
            }

            // 查询SKU参与的营销活动（如秒杀）
            ActivitySku promotion = activityService.findActiveSku(cart.getSkuId());
            // 若参与活动且活动库存不足，抛出异常
            if (promotion != null && promotion.getSeckillStock() != null
                    && promotion.getSeckillStock() < cart.getQuantity()) {
                throw new BusinessException("活动库存不足");
            }

            // 确定最终结算单价：若有活动且包含活动价则取活动价，否则取SKU原价
            BigDecimal price = promotion == null || promotion.getSeckillPrice() == null
                    ? sku.getPrice() : promotion.getSeckillPrice();
            // 计算当前商品小计金额 = 单价 * 购买数量
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(cart.getQuantity()));
            // 累加到商品总金额
            total = total.add(subtotal);

            // 处理币种：若SKU未设定币种则默认为USD，并校验整个结算单是否为混合币种
            String itemCurrency = sku.getCurrency() == null || sku.getCurrency().isBlank() ? "USD" : sku.getCurrency();
            if (currency == null) currency = itemCurrency; // 首次设定基准币种
            else if (!currency.equalsIgnoreCase(itemCurrency)) throw new BusinessException("暂不支持混合币种结算"); // 存在不同币种则拒绝结算

            // 查询SPU信息，用于获取类目和原产地以计算税费
            Spu spu = sku.getSpuId() == null ? null : spuMapper.selectById(sku.getSpuId());
            // 根据商品类目、原产国及收货国家获取适用的税率百分比
            BigDecimal taxRate = spu == null ? BigDecimal.ZERO
                    : taxConfigService.findApplicableRate(spu.getCategoryId(), spu.getOriginCountry(), address.getCountry());
            if (taxRate == null) taxRate = BigDecimal.ZERO;
            // 计算单项税费 = 小计金额 * (税率 / 100)，保留两位小数，四舍五入
            BigDecimal itemTax = subtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            // 累加到总税费
            taxAmount = taxAmount.add(itemTax);

            // 封装当前商品的结算明细信息
            Map<String, Object> item = new HashMap<>();
            item.put("cartId", cart.getId()); item.put("skuId", sku.getId()); item.put("skuCode", sku.getSkuCode());
            item.put("quantity", cart.getQuantity()); item.put("price", price); item.put("originalPrice", sku.getPrice());
            item.put("activityId", promotion == null ? null : promotion.getActivityId()); item.put("subtotal", subtotal);
            item.put("currency", itemCurrency); item.put("taxRate", taxRate); item.put("taxAmount", itemTax);
            items.add(item);
        }

        // 4. 处理优惠券：若未选择优惠券则折扣为0；否则校验优惠券有效性并计算折扣金额
        CouponService.DiscountResult coupon = couponId == null
                ? new CouponService.DiscountResult(BigDecimal.ZERO.setScale(2), null)
                : couponService.validateAndCalculateDiscount(userId, couponId, total);

        // 5. 筛选用户当前可用的优惠券列表：状态为未使用(0)且满足门槛金额要求
        BigDecimal settledTotal = total;
        List<MemberCouponVO> availableCoupons = couponService.listMemberCoupons(userId).stream()
                .filter(item -> Integer.valueOf(0).equals(item.getStatus()))
                .filter(item -> item.getThreshold() == null || settledTotal.compareTo(item.getThreshold()) >= 0)
                .toList();

        // 6. 组装最终结算预览结果
        Map<String, Object> result = new HashMap<>();
        result.put("items", items); // 结算商品明细列表
        result.put("totalAmount", total); // 商品总金额（优惠前）
        result.put("discountAmount", coupon.amount()); // 优惠券抵扣金额
        result.put("freightAmount", BigDecimal.ZERO); // 运费（当前暂无，设为0）
        result.put("taxAmount", taxAmount.setScale(2, RoundingMode.HALF_UP)); // 总税费
        result.put("currency", currency); // 统一结算币种
        // 实际应付金额 = 商品总金额 - 优惠抵扣金额 + 总税费
        result.put("payAmount", total.subtract(coupon.amount()).add(taxAmount).setScale(2, RoundingMode.HALF_UP));
        result.put("availableCoupons", availableCoupons); // 当前可用优惠券列表
        result.put("address", address); // 收货地址信息
        return result;
    }

}
