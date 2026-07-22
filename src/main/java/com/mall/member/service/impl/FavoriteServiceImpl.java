package com.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BusinessException;
import com.mall.member.entity.MemberFavorite;
import com.mall.member.mapper.MemberFavoriteMapper;
import com.mall.member.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final MemberFavoriteMapper favoriteMapper;

    @Override
    public void add(Long userId, Long spuId) {
        Long count = favoriteMapper.selectCount(
                Wrappers.lambdaQuery(MemberFavorite.class)
                        .eq(MemberFavorite::getUserId, userId)
                        .eq(MemberFavorite::getSpuId, spuId));
        if (count > 0) throw new BusinessException("已收藏");
        MemberFavorite fav = new MemberFavorite();
        fav.setUserId(userId);
        fav.setSpuId(spuId);
        favoriteMapper.insert(fav);
    }

    @Override
    public void delete(Long userId, Long spuId) {
        favoriteMapper.delete(
                Wrappers.lambdaQuery(MemberFavorite.class)
                        .eq(MemberFavorite::getUserId, userId)
                        .eq(MemberFavorite::getSpuId, spuId));
    }

    @Override
    public IPage<MemberFavorite> selectPage(Integer page, Integer size, Long userId) {
        LambdaQueryWrapper<MemberFavorite> qw = Wrappers.lambdaQuery(MemberFavorite.class)
                .eq(MemberFavorite::getUserId, userId)
                .orderByDesc(MemberFavorite::getCreateTime);
        return favoriteMapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public boolean isFavorited(Long userId, Long spuId) {
        Long count = favoriteMapper.selectCount(
                Wrappers.lambdaQuery(MemberFavorite.class)
                        .eq(MemberFavorite::getUserId, userId)
                        .eq(MemberFavorite::getSpuId, spuId));
        return count > 0;
    }
}
