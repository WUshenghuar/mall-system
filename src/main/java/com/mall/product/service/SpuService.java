package com.mall.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.product.entity.Spu;

public interface SpuService {
    IPage<Spu> selectPage(Integer page, Integer size, Long categoryId, Integer status, String keyword);
    Spu getById(Long id);
    void save(Spu spu);
    void update(Spu spu);
    void delete(Long id);
    void publish(Long id, Integer status);
}
