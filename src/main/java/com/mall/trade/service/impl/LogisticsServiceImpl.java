package com.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.exception.BusinessException;
import com.mall.trade.entity.TradeLogistics;
import com.mall.trade.mapper.TradeLogisticsMapper;
import com.mall.trade.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogisticsServiceImpl implements LogisticsService {

    private final TradeLogisticsMapper logisticsMapper;

    @Override
    public TradeLogistics getByOrderNo(String orderNo) {
        LambdaQueryWrapper<TradeLogistics> qw = Wrappers.lambdaQuery(TradeLogistics.class)
                .eq(TradeLogistics::getOrderNo, orderNo);
        return logisticsMapper.selectOne(qw);
    }

    @Override
    public void ship(String orderNo, String logisticsNo, String logisticsCompany) {
        TradeLogistics logistics = getByOrderNo(orderNo);
        if (logistics == null) {
            logistics = new TradeLogistics();
            logistics.setOrderNo(orderNo);
        }
        logistics.setLogisticsNo(logisticsNo);
        logistics.setLogisticsCompany(logisticsCompany);
        logistics.setLogisticsStatus(1);
        if (logistics.getId() == null) {
            logisticsMapper.insert(logistics);
        } else {
            logisticsMapper.updateById(logistics);
        }
    }

    @Override
    public void updateLogisticsInfo(String orderNo, String logisticsInfo) {
        TradeLogistics logistics = getByOrderNo(orderNo);
        if (logistics == null) throw new BusinessException("物流信息不存在");
        logistics.setLogisticsInfo(logisticsInfo);
        logisticsMapper.updateById(logistics);
    }
}
