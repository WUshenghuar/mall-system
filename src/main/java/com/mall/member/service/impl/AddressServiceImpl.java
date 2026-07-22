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

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final MemberAddressMapper addressMapper;

    @Override
    public List<MemberAddress> getList(Long userId) {
        LambdaQueryWrapper<MemberAddress> qw = Wrappers.lambdaQuery(MemberAddress.class)
                .eq(MemberAddress::getUserId, userId)
                .orderByDesc(MemberAddress::getIsDefault)
                .orderByDesc(MemberAddress::getCreateTime);
        return addressMapper.selectList(qw);
    }

    @Override
    public MemberAddress getById(Long id) {
        return addressMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(MemberAddress address) {
        long count = addressMapper.selectCount(
                Wrappers.lambdaQuery(MemberAddress.class).eq(MemberAddress::getUserId, address.getUserId()));
        if (count == 0) address.setIsDefault(1);
        addressMapper.insert(address);
    }

    @Override
    public void update(MemberAddress address) {
        addressMapper.updateById(address);
    }

    @Override
    public void delete(Long id) {
        addressMapper.deleteById(id);
    }

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
