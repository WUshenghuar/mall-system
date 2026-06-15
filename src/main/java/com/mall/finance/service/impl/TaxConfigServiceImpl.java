package com.mall.finance.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.finance.entity.TaxConfig;
import com.mall.finance.mapper.TaxConfigMapper;
import com.mall.finance.service.TaxConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaxConfigServiceImpl implements TaxConfigService {
    private final TaxConfigMapper taxConfigMapper;

    @Override
    public IPage<TaxConfig> selectPage(Integer page, Integer size) {
        return taxConfigMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<TaxConfig>lambdaQuery().orderByDesc(TaxConfig::getCreateTime));
    }

    @Override
    public TaxConfig getById(Long id) {
        return taxConfigMapper.selectById(id);
    }

    @Override
    public void save(TaxConfig config) {
        taxConfigMapper.insert(config);
    }

    @Override
    public void update(TaxConfig config) {
        taxConfigMapper.updateById(config);
    }

    @Override
    public void delete(Long id) {
        taxConfigMapper.deleteById(id);
    }
}
