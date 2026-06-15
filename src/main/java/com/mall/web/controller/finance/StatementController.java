package com.mall.web.controller.finance;

import com.alibaba.excel.EasyExcel;
import com.mall.common.result.Result;
import com.mall.finance.entity.StatementExportVO;
import com.mall.finance.service.StatementService;
import com.mall.finance.service.TaxConfigService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class StatementController {
    private final StatementService statementService;
    private final TaxConfigService taxConfigService;

    @GetMapping("/statement/page")
    @PreAuthorize("hasAuthority('finance:statement:list')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(statementService.selectPage(page, size));
    }

    @GetMapping("/statement/{id}")
    @PreAuthorize("hasAuthority('finance:statement:list')")
    public Result<Object> detail(@PathVariable Long id) {
        return Result.success(statementService.getDetail(id));
    }

    @GetMapping("/statement/{id}/export")
    @PreAuthorize("hasAuthority('finance:statement:export')")
    public void export(@PathVariable Long id, HttpServletResponse response) throws IOException {
        List<StatementExportVO> data = statementService.getExportData(id);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = URLEncoder.encode("对账单.xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        EasyExcel.write(response.getOutputStream(), StatementExportVO.class)
                .sheet("对账单").doWrite(data);
    }

    @PutMapping("/statement/{id}/confirm")
    @PreAuthorize("hasAuthority('finance:statement:confirm')")
    public Result<Void> confirm(@PathVariable Long id) {
        statementService.confirm(id);
        return Result.success(null);
    }

    @GetMapping("/tax/page")
    @PreAuthorize("hasAuthority('finance:tax:config')")
    public Result<Object> taxPage(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(taxConfigService.selectPage(page, size));
    }

    @PostMapping("/tax")
    @PreAuthorize("hasAuthority('finance:tax:config')")
    public Result<Void> addTax(@RequestBody com.mall.finance.entity.TaxConfig config) {
        taxConfigService.save(config);
        return Result.success(null);
    }

    @PutMapping("/tax/{id}")
    @PreAuthorize("hasAuthority('finance:tax:config')")
    public Result<Void> updateTax(@PathVariable Long id,
                                   @RequestBody com.mall.finance.entity.TaxConfig config) {
        config.setId(id);
        taxConfigService.update(config);
        return Result.success(null);
    }

    @DeleteMapping("/tax/{id}")
    @PreAuthorize("hasAuthority('finance:tax:config')")
    public Result<Void> deleteTax(@PathVariable Long id) {
        taxConfigService.delete(id);
        return Result.success(null);
    }
}
