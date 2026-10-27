package com.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.exception.BusinessException;
import com.mall.trade.entity.TradeLogistics;
import com.mall.trade.mapper.TradeLogisticsMapper;
import com.mall.trade.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 维护订单物流记录，并允许发货时创建或更新对应记录。 */
/**
 * 物流服务实现类
 * 提供物流相关的业务操作，包括查询物流信息、创建/更新物流记录等
 */
@Service // 标识为Spring服务层组件
@RequiredArgsConstructor // 使用Lombok生成构造器，自动注入final字段
public class LogisticsServiceImpl implements LogisticsService {

    // 物流数据访问层，用于与数据库交互
    private final TradeLogisticsMapper logisticsMapper;

    /**
     * 根据订单号查询物流信息
     * @param orderNo 订单号
     * @return 对应的物流记录，如果没有则返回null
     */
    @Override
    public TradeLogistics getByOrderNo(String orderNo) {
        // 使用Lambda构造查询条件，查询指定订单号的物流记录
        LambdaQueryWrapper<TradeLogistics> qw = Wrappers.lambdaQuery(TradeLogistics.class)
                .eq(TradeLogistics::getOrderNo, orderNo);
        return logisticsMapper.selectOne(qw);
    }

    /** 首次发货时创建物流记录，重复调用则更新现有运单信息。 */
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

    /**
     * 更新物流信息
     * @param orderNo 订单编号
     * @param logisticsInfo 物流信息
     */
    @Override
    public void updateLogisticsInfo(String orderNo, String logisticsInfo) {
        // 根据订单编号查询物流信息
        TradeLogistics logistics = getByOrderNo(orderNo);
        // 如果物流信息不存在，则抛出业务异常
        if (logistics == null) throw new BusinessException("物流信息不存在");
        // 设置新的物流信息
        logistics.setLogisticsInfo(logisticsInfo);
        // 更新物流信息到数据库
        logisticsMapper.updateById(logistics);
    }
}
