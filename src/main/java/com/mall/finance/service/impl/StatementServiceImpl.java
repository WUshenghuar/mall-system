package com.mall.finance.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BusinessException;
import com.mall.finance.entity.Statement;
import com.mall.finance.entity.StatementExportVO;
import com.mall.finance.entity.StatementItem;
import com.mall.finance.mapper.StatementItemMapper;
import com.mall.finance.mapper.StatementMapper;
import com.mall.finance.service.StatementService;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.entity.TradeRefund;
import com.mall.trade.mapper.TradeOrderMapper;
import com.mall.trade.mapper.TradeRefundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatementServiceImpl implements StatementService {
    private final StatementMapper statementMapper;
    private final StatementItemMapper itemMapper;
    private final TradeOrderMapper orderMapper;
    private final TradeRefundMapper refundMapper;

    @Override
    public IPage<Statement> selectPage(Integer page, Integer size) {
        return statementMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<Statement>lambdaQuery().orderByDesc(Statement::getCreateTime));
    }

    @Override
    @Transactional
    public void generateCurrentMonth() {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = start.plusMonths(1);
        if (statementMapper.selectCount(Wrappers.<Statement>lambdaQuery()
                .eq(Statement::getPeriodStart, start).eq(Statement::getPeriodEnd, end.minusDays(1))) > 0) {
            throw new BusinessException("本月对账单已生成");
        }
        List<TradeOrder> orders = orderMapper.selectList(Wrappers.<TradeOrder>lambdaQuery()
                .in(TradeOrder::getOrderStatus, 1, 2, 3, 6)
                .ge(TradeOrder::getCreateTime, start.atStartOfDay())
                .lt(TradeOrder::getCreateTime, end.atStartOfDay()));
        if (orders.isEmpty()) throw new BusinessException("本月没有可对账的已支付订单");
        Map<String, BigDecimal> refunds = refundMapper.selectList(Wrappers.<TradeRefund>lambdaQuery()
                        .eq(TradeRefund::getRefundStatus, 3)
                        .in(TradeRefund::getOrderNo, orders.stream().map(TradeOrder::getOrderNo).toList()))
                .stream().collect(Collectors.toMap(TradeRefund::getOrderNo, TradeRefund::getRefundAmount, BigDecimal::add));
        Statement statement = new Statement();
        statement.setStatementNo("ST" + start.toString().replace("-", ""));
        statement.setPeriodStart(start);
        statement.setPeriodEnd(end.minusDays(1));
        statement.setTotalAmount(orders.stream().map(TradeOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        statement.setTariffAmount(BigDecimal.ZERO);
        statement.setShippingFee(orders.stream().map(TradeOrder::getFreightAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        statement.setRefundAmount(refunds.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        statement.setNetAmount(orders.stream().map(TradeOrder::getPayAmount).reduce(BigDecimal.ZERO, BigDecimal::add).subtract(statement.getRefundAmount()));
        statement.setOrderCount(orders.size());
        statement.setStatus(0);
        statementMapper.insert(statement);
        for (TradeOrder order : orders) {
            StatementItem item = new StatementItem();
            item.setStatementId(statement.getId());
            item.setOrderNo(order.getOrderNo());
            item.setTotalAmount(order.getTotalAmount());
            item.setTariffAmount(BigDecimal.ZERO);
            item.setShippingFee(order.getFreightAmount());
            item.setRefundAmount(refunds.getOrDefault(order.getOrderNo(), BigDecimal.ZERO));
            item.setPayAmount(order.getPayAmount());
            item.setOrderTime(order.getCreateTime());
            itemMapper.insert(item);
        }
    }

    @Override
    public Map<String, Object> getDetail(Long id) {
        Statement stmt = statementMapper.selectById(id);
        if (stmt == null) return Collections.emptyMap();
        List<StatementItem> items = itemMapper.selectList(
                Wrappers.<StatementItem>lambdaQuery()
                        .eq(StatementItem::getStatementId, id));
        Map<String, Object> result = new HashMap<>();
        result.put("statement", stmt);
        result.put("items", items);
        return result;
    }

    @Override
    public List<StatementExportVO> getExportData(Long id) {
        List<StatementItem> items = itemMapper.selectList(
                Wrappers.<StatementItem>lambdaQuery()
                        .eq(StatementItem::getStatementId, id));
        return items.stream().map(item -> {
            StatementExportVO vo = new StatementExportVO();
            vo.setOrderNo(item.getOrderNo());
            vo.setTotalAmount(item.getTotalAmount());
            vo.setTariffAmount(item.getTariffAmount());
            vo.setShippingFee(item.getShippingFee());
            vo.setRefundAmount(item.getRefundAmount());
            vo.setPayAmount(item.getPayAmount());
            vo.setCreateTime(item.getOrderTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void confirm(Long id) {
        Statement stmt = statementMapper.selectById(id);
        if (stmt == null) throw new BusinessException("对账单不存在");
        stmt.setStatus(1);
        statementMapper.updateById(stmt);
    }
}
