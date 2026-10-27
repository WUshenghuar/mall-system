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

/** 按自然月汇总订单与已完成退款，生成可查询和导出的财务对账单。 */
/**
 * 对账单服务实现类
 * 实现了对账单的查询、生成、详情获取、导出和确认等功能
 */
/**
 * 对账单服务实现类
 * 提供对账单的生成、查询、确认等功能
 */
@Service
@RequiredArgsConstructor
public class StatementServiceImpl implements StatementService {
    // 注入对账单数据访问层
    private final StatementMapper statementMapper;
    // 注入对账单项数据访问层
    private final StatementItemMapper itemMapper;
    private final TradeOrderMapper orderMapper;
    private final TradeRefundMapper refundMapper;

    @Override
    public IPage<Statement> selectPage(Integer page, Integer size) {
        return statementMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<Statement>lambdaQuery().orderByDesc(Statement::getCreateTime));
    }

    /** 以订单创建时间归属当前自然月；整张对账单及明细在一个事务内生成。 */
    /**
     * 生成当前月份的对账单
     * 该方法使用事务管理，确保数据一致性
     * @throws BusinessException 如果当月对账单已存在或没有可对账的已支付订单
     */
    @Override
    @Transactional
    public void generateCurrentMonth() {
        // 获取当前月份的第一天
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        // 获取下个月的第一天，用于确定对账单的结束日期
        LocalDate end = start.plusMonths(1);
        // 检查当月对账单是否已存在
        if (statementMapper.selectCount(Wrappers.<Statement>lambdaQuery()
                .eq(Statement::getPeriodStart, start).eq(Statement::getPeriodEnd, end.minusDays(1))) > 0) {
            throw new BusinessException("本月对账单已生成");
        }
        // 查询符合条件的订单状态为1,2,3,6的订单，且创建时间在本月内
        List<TradeOrder> orders = orderMapper.selectList(Wrappers.<TradeOrder>lambdaQuery()
                .in(TradeOrder::getOrderStatus, 1, 2, 3, 6)
                .ge(TradeOrder::getCreateTime, start.atStartOfDay())
                .lt(TradeOrder::getCreateTime, end.atStartOfDay()));
        // 如果没有符合条件的订单，抛出异常
        if (orders.isEmpty()) throw new BusinessException("本月没有可对账的已支付订单");
        // 查询所有符合条件的退款记录，并按订单号汇总退款金额
        Map<String, BigDecimal> refunds = refundMapper.selectList(Wrappers.<TradeRefund>lambdaQuery()
                        .eq(TradeRefund::getRefundStatus, 3)
                        .in(TradeRefund::getOrderNo, orders.stream().map(TradeOrder::getOrderNo).toList()))
                .stream().collect(Collectors.toMap(TradeRefund::getOrderNo, TradeRefund::getRefundAmount, BigDecimal::add));
        // 创建对账单对象
        Statement statement = new Statement();
        // 设置对账单号，格式为ST+年月日（无分隔符）
        statement.setStatementNo("ST" + start.toString().replace("-", ""));
        statement.setPeriodStart(start);
        statement.setPeriodEnd(end.minusDays(1));
        // 计算并设置总金额
        statement.setTotalAmount(orders.stream().map(TradeOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        // 计算并设置税费金额
        statement.setTariffAmount(orders.stream().map(order -> order.getTaxAmount() == null ? BigDecimal.ZERO : order.getTaxAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        // 计算并设置运费
        statement.setShippingFee(orders.stream().map(TradeOrder::getFreightAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        // 设置退款金额
        statement.setRefundAmount(refunds.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        // 计算并设置净金额（支付总额减去退款金额）
        statement.setNetAmount(orders.stream().map(TradeOrder::getPayAmount).reduce(BigDecimal.ZERO, BigDecimal::add).subtract(statement.getRefundAmount()));
        // 设置订单数量
        statement.setOrderCount(orders.size());
        // 设置对账单状态为0（可能表示初始状态）
        statement.setStatus(0);
        // 插入对账单记录
        statementMapper.insert(statement);
        // 遍历所有订单，创建对账单明细
        for (TradeOrder order : orders) {
            StatementItem item = new StatementItem();
            item.setStatementId(statement.getId());
            item.setOrderNo(order.getOrderNo());
            item.setTotalAmount(order.getTotalAmount());
            item.setTariffAmount(order.getTaxAmount() == null ? BigDecimal.ZERO : order.getTaxAmount());
            item.setShippingFee(order.getFreightAmount());
            item.setRefundAmount(refunds.getOrDefault(order.getOrderNo(), BigDecimal.ZERO));
            item.setPayAmount(order.getPayAmount());
            item.setOrderTime(order.getCreateTime());
            // 插入对账单明细记录
            itemMapper.insert(item);
        }
    }

    /**
     * 根据ID获取单据详情信息
     * @param id 单据ID
     * @return 包含单据信息和单据项目的Map，如果单据不存在则返回空Map
     */
    @Override
    public Map<String, Object> getDetail(Long id) {
        // 根据ID查询单据信息
        Statement stmt = statementMapper.selectById(id);
        // 如果单据不存在，返回空Map
        if (stmt == null) return Collections.emptyMap();
        // 查询该单据下的所有单据项目
        List<StatementItem> items = itemMapper.selectList(
                Wrappers.<StatementItem>lambdaQuery()
                        .eq(StatementItem::getStatementId, id));
        // 构建返回结果Map，包含单据信息和单据项目列表
        Map<String, Object> result = new HashMap<>();
        result.put("statement", stmt);
        result.put("items", items);
        return result;
    }

    /**
     * 获取导出数据的方法
     * @param id 账单ID
     * @return 返回StatementExportVO对象列表，用于数据导出
     */
    @Override
    public List<StatementExportVO> getExportData(Long id) {
        // 根据账单ID查询所有账单项
        List<StatementItem> items = itemMapper.selectList(
                Wrappers.<StatementItem>lambdaQuery()
                        .eq(StatementItem::getStatementId, id));
        // 使用Stream流将StatementItem转换为StatementExportVO对象
        return items.stream().map(item -> {
            StatementExportVO vo = new StatementExportVO();
            // 设置订单号
            vo.setOrderNo(item.getOrderNo());
            // 设置总金额
            vo.setTotalAmount(item.getTotalAmount());
            // 设置关税金额
            vo.setTariffAmount(item.getTariffAmount());
            // 设置运费
            vo.setShippingFee(item.getShippingFee());
            // 设置退款金额
            vo.setRefundAmount(item.getRefundAmount());
            // 设置支付金额
            vo.setPayAmount(item.getPayAmount());
            // 设置创建时间（使用订单时间）
            vo.setCreateTime(item.getOrderTime());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 确认对账单的方法
     * @param id 对账单ID
     * @Transactional 注解表明此方法在事务中执行
     */
    @Override
    @Transactional
    public void confirm(Long id) {
        // 根据ID查询对账单信息
        Statement stmt = statementMapper.selectById(id);
        // 如果对账单不存在，抛出业务异常
        if (stmt == null) throw new BusinessException("对账单不存在");
        // 设置对账单状态为1（已确认）
        stmt.setStatus(1);
        // 更新对账单信息
        statementMapper.updateById(stmt);
    }
}
