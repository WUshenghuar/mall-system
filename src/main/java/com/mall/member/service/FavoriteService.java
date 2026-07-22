package com.mall.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.member.entity.MemberFavorite;

public interface FavoriteService {
    void add(Long userId, Long spuId);
    void delete(Long userId, Long spuId);
    IPage<MemberFavorite> selectPage(Integer page, Integer size, Long userId);
    boolean isFavorited(Long userId, Long spuId);
}
