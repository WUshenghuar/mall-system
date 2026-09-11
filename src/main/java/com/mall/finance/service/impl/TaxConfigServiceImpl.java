package com.mall.finance.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.finance.entity.TaxConfig;
import com.mall.finance.mapper.TaxConfigMapper;
import com.mall.finance.service.TaxConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public BigDecimal findApplicableRate(Long categoryId, String originCountry, String destCountry) {
        LocalDate today = LocalDate.now();
        Map<String, TaxConfig> selected = new HashMap<>();
        Map<String, Integer> scores = new HashMap<>();
        List<TaxConfig> configs = taxConfigMapper.selectList(
                Wrappers.<TaxConfig>lambdaQuery().orderByDesc(TaxConfig::getEffectiveDate));
        for (TaxConfig config : configs) {
            if (!active(config, today) || !matches(config.getCategoryId(), categoryId)
                    || !matches(config.getOriginCountry(), originCountry)
                    || !matches(config.getDestCountry(), destCountry)) continue;
            String type = config.getTaxType() == null ? "" : config.getTaxType();
            int score = specificity(config);
            if (score > scores.getOrDefault(type, -1)) {
                selected.put(type, config);
                scores.put(type, score);
            }
        }
        return selected.values().stream().map(TaxConfig::getTaxRate).filter(rate -> rate != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean active(TaxConfig config, LocalDate today) {
        return (config.getEffectiveDate() == null || !config.getEffectiveDate().isAfter(today))
                && (config.getExpireDate() == null || !config.getExpireDate().isBefore(today));
    }

    private boolean matches(Long configured, Long actual) {
        return configured == null || configured.equals(actual);
    }

    private boolean matches(String configured, String actual) {
        return configured == null || configured.isBlank()
                || (actual != null && configured.equalsIgnoreCase(actual));
    }

    private int specificity(TaxConfig config) {
        return (config.getCategoryId() == null ? 0 : 1)
                + (config.getOriginCountry() == null || config.getOriginCountry().isBlank() ? 0 : 1)
                + (config.getDestCountry() == null || config.getDestCountry().isBlank() ? 0 : 1);
    }
}
