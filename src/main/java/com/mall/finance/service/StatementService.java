package com.mall.finance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.finance.entity.Statement;
import com.mall.finance.entity.StatementExportVO;

import java.util.List;
import java.util.Map;

public interface StatementService {
    IPage<Statement> selectPage(Integer page, Integer size);
    Map<String, Object> getDetail(Long id);
    List<StatementExportVO> getExportData(Long id);
    void confirm(Long id);
}
