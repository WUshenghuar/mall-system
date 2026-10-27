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

/** 管理税率配置，并按商品、来源地和目的地匹配适用税率。 */
/**
 * 税费配置服务实现类
 * 实现了税费配置的增删改查以及适用税率的计算功能
 */
@Service
@RequiredArgsConstructor
public class TaxConfigServiceImpl implements TaxConfigService {
    // 使用final确保依赖注入的不可变性
    private final TaxConfigMapper taxConfigMapper;

    @Override
    public IPage<TaxConfig> selectPage(Integer page, Integer size) {
        return taxConfigMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<TaxConfig>lambdaQuery().orderByDesc(TaxConfig::getCreateTime));
    }

    /**
     * 根据ID获取税务配置信息
     * @param id 税务配置的唯一标识符
     * @return 返回对应ID的TaxConfig对象，如果不存在则返回null
     */
    @Override
    public TaxConfig getById(Long id) {
        // 使用MyBatis-Plus的selectById方法从数据库中查询指定ID的税务配置信息
        return taxConfigMapper.selectById(id);
    }

    /**
     * 保存税务配置信息
     * @param config 税务配置对象，包含需要保存的配置信息
     * @Override 注解表明此方法是重写父类的方法
     */
    @Override  // 标记此方法是重写父类的方法
    public void save(TaxConfig config) {
        taxConfigMapper.insert(config);
    }

    /**
     * 更新税务配置信息
     * @param config 包含更新后税务配置信息的对象
     */
    @Override
    public void update(TaxConfig config) {
        // 通过调用Mapper的updateById方法来更新数据库中的税务配置记录
        taxConfigMapper.updateById(config);
    }

    /**
     * 删除指定ID的税务配置信息
     * @param id 要删除的税务配置ID
     */
    @Override
    public void delete(Long id) {  // 实现删除税务配置的方法
        taxConfigMapper.deleteById(id);  // 调用Mapper层的删除方法，根据ID删除税务配置
    }

    /** 每种税种选取匹配维度最具体且处于有效期的配置，再汇总其税率。 */
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
