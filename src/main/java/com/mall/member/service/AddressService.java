package com.mall.member.service;

import com.mall.member.entity.MemberAddress;

import java.util.List;

public interface AddressService {
    List<MemberAddress> getList(Long userId);
    MemberAddress getById(Long id);
    void add(MemberAddress address);
    void update(MemberAddress address);
    void delete(Long id);
    void setDefault(Long id, Long userId);
}
