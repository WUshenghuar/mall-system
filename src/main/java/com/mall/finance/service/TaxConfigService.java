package com.mall.finance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.finance.entity.TaxConfig;

public interface TaxConfigService {
    IPage<TaxConfig> selectPage(Integer page, Integer size);
    TaxConfig getById(Long id);
    void save(TaxConfig config);
    void update(TaxConfig config);
    void delete(Long id);
}
