package com.mall.web.controller.system;

import com.mall.common.result.Result;
import com.mall.member.mapper.MemberMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.security.user.LoginUser;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 提供后台经营统计和实时刷新流，并限制仅后台用户访问。 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    /** 提供商品总量统计。 */
    private final SpuMapper spuMapper;
    /** 查询订单数量、时段分布及实收金额统计。 */
    private final TradeOrderMapper tradeOrderMapper;
    /** 提供会员数量统计。 */
    private final MemberMapper memberMapper;
    /** 提供待处理退款数量统计。 */
    private final com.mall.trade.mapper.TradeRefundMapper refundMapper;
    /** 管理仪表盘刷新 SSE 订阅及消息广播。 */
    private final DashboardRefreshHub refreshHub;

    /** 建立仪表盘实时刷新 SSE 连接；仅接受后台认证主体。 */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication auth) {
        requireStaff(auth);
        return refreshHub.subscribe();
    }

    /**
     * 汇总仪表盘经营指标及逐小时订单数。
     *
     * <p>range 为 24h 时统计最近 24 个整点小时，否则按当天起始时间统计；
     * 结果包含商品、订单、会员、待退款数量、实收金额及小时标签和订单计数。</p>
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam(defaultValue = "today") String range, Authentication auth) {
        requireStaff(auth);

        // 以本地日期零点作为“今天”的起点；最近 24 小时模式则对齐到整点。
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        boolean recent24Hours = "24h".equals(range);
        LocalDateTime start = recent24Hours ? LocalDateTime.now().minusHours(23).withMinute(0).withSecond(0).withNano(0) : todayStart;

        // 统计卡片的汇总值；实收金额为空时以零值返回，避免前端处理空金额。
        long productCount = spuMapper.selectCount(null);
        long orderCount = tradeOrderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TradeOrder>()
                        .ge(TradeOrder::getCreateTime, start));
        long memberCount = memberMapper.selectCount(null);
        long pendingRefund = refundMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mall.trade.entity.TradeRefund>()
                        .eq(com.mall.trade.entity.TradeRefund::getRefundStatus, 0));
        BigDecimal paidAmount = tradeOrderMapper.sumPaidAmountSince(start);

        // 使用有序映射保持响应字段的稳定顺序，并预先为 24 个小时创建零计数。
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productCount", productCount);
        data.put("todayOrders", orderCount);
        data.put("memberCount", memberCount);
        data.put("pendingRefund", pendingRefund);
        data.put("paidAmount", paidAmount == null ? BigDecimal.ZERO : paidAmount);
        List<Long> hourlyOrderCounts = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) hourlyOrderCounts.add(0L);

        // 生成图表横轴标签，再将数据库按小时聚合的结果映射到对应位置。
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

    /** 阻止非后台认证主体访问仪表盘数据和实时刷新流。 */
    private void requireStaff(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser)) {
            throw new com.mall.common.exception.BusinessException(403, "仅后台账户可访问");
        }
    }
}
