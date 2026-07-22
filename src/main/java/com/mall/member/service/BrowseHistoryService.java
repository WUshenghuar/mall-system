package com.mall.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.member.entity.MemberBrowseHistory;

public interface BrowseHistoryService {
    void add(Long userId, Long spuId);
    IPage<MemberBrowseHistory> selectPage(Integer page, Integer size, Long userId);
}
