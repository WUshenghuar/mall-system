package com.mall.web.controller.system;

import com.mall.common.result.Result;
import com.mall.member.mapper.MemberMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final com.mall.trade.mapper.TradeRefundMapper refundMapper;
    private final DashboardRefreshHub refreshHub;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return refreshHub.subscribe();
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam(defaultValue = "today") String range) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        boolean recent24Hours = "24h".equals(range);
        LocalDateTime start = recent24Hours ? LocalDateTime.now().minusHours(23).withMinute(0).withSecond(0).withNano(0) : todayStart;

        long productCount = spuMapper.selectCount(null);
        long orderCount = tradeOrderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TradeOrder>()
                        .ge(TradeOrder::getCreateTime, start));
        long memberCount = memberMapper.selectCount(null);
        long pendingRefund = refundMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mall.trade.entity.TradeRefund>()
                        .eq(com.mall.trade.entity.TradeRefund::getRefundStatus, 0));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productCount", productCount);
        data.put("todayOrders", orderCount);
        data.put("memberCount", memberCount);
        data.put("pendingRefund", pendingRefund);
        List<Long> hourlyOrderCounts = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) hourlyOrderCounts.add(0L);
        List<String> hourLabels = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");
        for (int hour = 0; hour < 24; hour++) hourLabels.add((recent24Hours ? start.plusHours(hour) : todayStart.plusHours(hour)).format(formatter));
        for (Map<String, Object> row : tradeOrderMapper.countByHourSince(start)) {
            String hourKey = String.valueOf(row.get("hourKey"));
            int index = hourLabels.indexOf(hourKey);
            if (index >= 0) hourlyOrderCounts.set(index, ((Number) row.get("count")).longValue());
        }
        data.put("hourlyOrderCounts", hourlyOrderCounts);
        data.put("hourLabels", hourLabels);
        data.put("range", recent24Hours ? "24h" : "today");
        return Result.success(data);
    }
}
