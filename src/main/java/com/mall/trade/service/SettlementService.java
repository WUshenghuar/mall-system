package com.mall.trade.service;

import java.util.List;
import java.util.Map;

public interface SettlementService {
    Map<String, Object> preview(Long userId, List<Long> cartIds, Long addressId);
}
