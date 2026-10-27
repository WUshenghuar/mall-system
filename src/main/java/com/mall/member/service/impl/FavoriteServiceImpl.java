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

/** 管理会员对 SPU 的收藏关系，并提供按会员隔离的收藏列表。 */
/**
 * 收藏服务实现类
 * 提供商品收藏相关的功能，包括添加收藏、删除收藏、查询收藏列表和判断是否收藏等
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    /**
     * 会员收藏数据访问层
     * 用于处理与收藏相关的数据库操作
     */
    private final MemberFavoriteMapper favoriteMapper;

    /** 发现已有收藏时拒绝新增。 */
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

    /**
     * 删除用户收藏的商品
     * @param userId 用户ID
     * @param spuId 商品SPU ID
     */
    @Override
    public void delete(Long userId, Long spuId) {
        // 使用MyBatis-Plus的Lambda查询构造器构建删除条件
        // 删除条件为：用户ID和商品SPU ID同时匹配的收藏记录
        favoriteMapper.delete(
                Wrappers.lambdaQuery(MemberFavorite.class)
                        .eq(MemberFavorite::getUserId, userId)  // 设置用户ID等于传入的userId参数
                        .eq(MemberFavorite::getSpuId, spuId)); // 设置商品SPU ID等于传入的spuId参数
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
