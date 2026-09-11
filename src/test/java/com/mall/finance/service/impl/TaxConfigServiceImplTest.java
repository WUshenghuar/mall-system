package com.mall.finance.service.impl;

import com.mall.finance.entity.TaxConfig;
import com.mall.finance.mapper.TaxConfigMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaxConfigServiceImplTest {
    @Test
    void selectsMostSpecificActiveRatePerTaxType() {
        TaxConfigMapper mapper = mock(TaxConfigMapper.class);
        TaxConfig genericVat = config(null, null, null, "VAT", "10");
        TaxConfig categoryVat = config(10L, null, null, "VAT", "5");
        TaxConfig countryDuty = config(10L, "CN", "US", "IMPORT_DUTY", "3");
        TaxConfig expired = config(10L, "CN", "US", "CONSUMPTION_TAX", "99");
        expired.setExpireDate(LocalDate.now().minusDays(1));
        when(mapper.selectList(any())).thenReturn(List.of(genericVat, categoryVat, countryDuty, expired));

        BigDecimal rate = new TaxConfigServiceImpl(mapper).findApplicableRate(10L, "CN", "US");

        assertThat(rate).isEqualByComparingTo("8");
    }

    private TaxConfig config(Long categoryId, String origin, String dest, String type, String rate) {
        TaxConfig config = new TaxConfig();
        config.setCategoryId(categoryId); config.setOriginCountry(origin); config.setDestCountry(dest);
        config.setTaxType(type); config.setTaxRate(new BigDecimal(rate));
        config.setEffectiveDate(LocalDate.now().minusDays(1));
        return config;
    }
}
