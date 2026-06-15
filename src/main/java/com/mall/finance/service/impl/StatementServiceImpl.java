package com.mall.finance.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.finance.entity.Statement;
import com.mall.finance.entity.StatementExportVO;
import com.mall.finance.entity.StatementItem;
import com.mall.finance.mapper.StatementItemMapper;
import com.mall.finance.mapper.StatementMapper;
import com.mall.finance.service.StatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public IPage<Statement> selectPage(Integer page, Integer size) {
        return statementMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<Statement>lambdaQuery().orderByDesc(Statement::getCreateTime));
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
        if (stmt == null) throw new RuntimeException("对账单不存在");
        stmt.setStatus(1);
        statementMapper.updateById(stmt);
    }
}
