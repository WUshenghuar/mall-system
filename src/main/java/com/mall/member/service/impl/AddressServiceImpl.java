package com.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.exception.BusinessException;
import com.mall.member.entity.MemberAddress;
import com.mall.member.mapper.MemberAddressMapper;
import com.mall.member.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 维护会员收货地址，并协调默认地址标记。 */
/**
 * 会员地址服务实现类
 * 提供会员地址的增删改查及默认地址设置等功能
 */
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    /**
     * 会员地址数据访问层
     * 使用final确保不可变，通过@RequiredArgsConstructor注入
     */
    private final MemberAddressMapper addressMapper;

    /**
     * 获取会员地址列表
     * @param userId 会员ID
     * @return 会员地址列表，按是否默认和创建时间降序排列
     */
    @Override
    public List<MemberAddress> getList(Long userId) {
        LambdaQueryWrapper<MemberAddress> qw = Wrappers.lambdaQuery(MemberAddress.class)
                .eq(MemberAddress::getUserId, userId)  // 按用户ID筛选
                .orderByDesc(MemberAddress::getIsDefault)  // 默认地址优先
                .orderByDesc(MemberAddress::getCreateTime);  // 按创建时间降序
        return addressMapper.selectList(qw);
    }

    /**
     * 根据ID获取会员地址
     * @param 地址ID
     * @return 会员地址对象
     */
    @Override
    public MemberAddress getById(Long id) {
        return addressMapper.selectById(id);
    }

    /** 会员首次新增地址时自动设为默认地址。 */
    /**
     * 添加会员地址信息
     * @param address 会员地址对象，包含地址详细信息
     * @Transactional(rollbackFor = Exception.class) 确保方法内所有异常都会触发事务回滚
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(MemberAddress address) {
        // 查询该用户已有的地址数量
        long count = addressMapper.selectCount(
                // 使用Lambda构造查询条件，查询指定用户的地址数量
                Wrappers.lambdaQuery(MemberAddress.class).eq(MemberAddress::getUserId, address.getUserId()));
        // 如果是用户的第一个地址，则设置为默认地址
        if (count == 0) address.setIsDefault(1);
        // 插入新的地址信息
        addressMapper.insert(address);
    }

    /**
     * 更新会员地址信息
     * @param address 包含会员地址信息的对象，其中应包含要更新的地址ID和其他相关信息
     */
    @Override
    public void update(MemberAddress address) {
        // 调用addressMapper的updateById方法，根据address对象中的ID更新对应的会员地址信息
        addressMapper.updateById(address);
    }

    /**
     * 根据ID删除地址信息
     * @param id 地址ID
     */
    @Override
    public void delete(Long id) {  // 实现删除地址的方法
        addressMapper.deleteById(id);  // 调用地址映射器的删除方法，根据ID删除地址
    }

    /** 在事务内清除该会员其他默认标记，再设定目标地址；调用前应由入口校验地址归属。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id, Long userId) {
        addressMapper.update(null, Wrappers.lambdaUpdate(MemberAddress.class)
                .eq(MemberAddress::getUserId, userId)
                .set(MemberAddress::getIsDefault, 0));
        MemberAddress address = addressMapper.selectById(id);
        if (address == null) throw new BusinessException("地址不存在");
        address.setIsDefault(1);
        addressMapper.updateById(address);
    }
}
