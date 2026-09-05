package com.mall.web.controller.system;

import com.mall.common.result.Result;
import com.mall.member.mapper.MemberMapper;
import com.mall.order.mapper.RefundMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final SpuMapper spuMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final MemberMapper memberMapper;
    private final RefundMapper refundMapper;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        long productCount = spuMapper.selectCount(null);
        long orderCount = tradeOrderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TradeOrder>()
                        .ge(TradeOrder::getCreateTime, todayStart));
        long memberCount = memberMapper.selectCount(null);
        long pendingRefund = refundMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mall.order.entity.OrderRefund>()
                        .eq(com.mall.order.entity.OrderRefund::getRefundStatus, 0));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productCount", productCount);
        data.put("todayOrders", orderCount);
        data.put("memberCount", memberCount);
        data.put("pendingRefund", pendingRefund);
        List<Long> hourlyOrderCounts = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) hourlyOrderCounts.add(0L);
        for (Map<String, Object> row : tradeOrderMapper.countByHourSince(todayStart)) {
            int hour = ((Number) row.get("hour")).intValue();
            hourlyOrderCounts.set(hour, ((Number) row.get("count")).longValue());
        }
        data.put("hourlyOrderCounts", hourlyOrderCounts);
        return Result.success(data);
    }
}
