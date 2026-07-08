package com.mall.web.controller.system;

import com.mall.common.result.Result;
import com.mall.member.mapper.MemberMapper;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mapper.RefundMapper;
import com.mall.product.mapper.SpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final SpuMapper spuMapper;
    private final OrderMapper orderMapper;
    private final MemberMapper memberMapper;
    private final RefundMapper refundMapper;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        long productCount = spuMapper.selectCount(null);
        long orderCount = orderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mall.order.entity.Order>()
                        .ge(com.mall.order.entity.Order::getCreateTime, todayStart));
        long memberCount = memberMapper.selectCount(null);
        long pendingRefund = refundMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mall.order.entity.OrderRefund>()
                        .eq(com.mall.order.entity.OrderRefund::getRefundStatus, 0));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productCount", productCount);
        data.put("todayOrders", orderCount);
        data.put("memberCount", memberCount);
        data.put("pendingRefund", pendingRefund);
        return Result.success(data);
    }
}
