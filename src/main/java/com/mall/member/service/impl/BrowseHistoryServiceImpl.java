package com.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.member.entity.MemberBrowseHistory;
import com.mall.member.mapper.MemberBrowseHistoryMapper;
import com.mall.member.service.BrowseHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrowseHistoryServiceImpl implements BrowseHistoryService {

    private final MemberBrowseHistoryMapper browseHistoryMapper;

    @Override
    public void add(Long userId, Long spuId) {
        MemberBrowseHistory history = new MemberBrowseHistory();
        history.setUserId(userId);
        history.setSpuId(spuId);
        browseHistoryMapper.insert(history);
    }

    @Override
    public IPage<MemberBrowseHistory> selectPage(Integer page, Integer size, Long userId) {
        LambdaQueryWrapper<MemberBrowseHistory> qw = Wrappers.lambdaQuery(MemberBrowseHistory.class)
                .eq(MemberBrowseHistory::getUserId, userId)
                .orderByDesc(MemberBrowseHistory::getCreateTime);
        return browseHistoryMapper.selectPage(new Page<>(page, size), qw);
    }
}
