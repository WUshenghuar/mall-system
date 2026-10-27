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

/**
 * 财务对账单与税率配置接口。
 *
 * <p>控制器负责接收 HTTP 请求、执行权限校验并将业务处理委托给对应服务；
 * 普通接口统一以 {@link Result} 返回，Excel 导出接口则直接写入 HTTP 响应流。</p>
 */
@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class StatementController {
    /** 处理对账单的查询、生成、明细和确认。 */
    private final StatementService statementService;
    /** 处理财务税率配置的查询和维护。 */
    private final TaxConfigService taxConfigService;

    /** 分页查询对账单；默认查询第 1 页，每页 10 条。 */
    @GetMapping("/statement/page")
    @PreAuthorize("hasAuthority('finance:statement:list')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(statementService.selectPage(page, size));
    }

    /** 触发生成当月对账单；生成规则及幂等处理由对账单服务负责。 */
    @PostMapping("/statement/generate")
    @PreAuthorize("hasAuthority('finance:statement:confirm')")
    public Result<Void> generate() {
        statementService.generateCurrentMonth();
        return Result.success(null);
    }

    /** 查询指定对账单及其明细，访问需要财务对账单查看权限。 */
    @GetMapping("/statement/{id}")
    @PreAuthorize("hasAuthority('finance:statement:list')")
    public Result<Object> detail(@PathVariable Long id) {
        return Result.success(statementService.getDetail(id));
    }

    /**
     * 将指定对账单导出为 XLSX 文件。
     *
     * <p>先由服务获取导出数据，再设置电子表格 MIME 类型、UTF-8 编码和下载文件名，
     * 最后通过 EasyExcel 将数据写入响应流。</p>
     */
    @GetMapping("/statement/{id}/export")
    @PreAuthorize("hasAuthority('finance:statement:export')")
    public void export(@PathVariable Long id, HttpServletResponse response) throws IOException {
        List<StatementExportVO> data = statementService.getExportData(id);
        // 声明 Excel 文件响应格式，并对文件名进行 UTF-8 URL 编码以兼容中文名称。
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = URLEncoder.encode("对账单.xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        // 使用导出视图对象确定表格列结构，将查询结果写入名为“对账单”的工作表。
        EasyExcel.write(response.getOutputStream(), StatementExportVO.class)
                .sheet("对账单").doWrite(data);
    }

    /** 确认指定对账单；具体状态校验和更新由对账单服务执行。 */
    @PutMapping("/statement/{id}/confirm")
    @PreAuthorize("hasAuthority('finance:statement:confirm')")
    public Result<Void> confirm(@PathVariable Long id) {
        statementService.confirm(id);
        return Result.success(null);
    }

    /** 分页查询税率配置；默认查询第 1 页，每页 10 条。 */
    @GetMapping("/tax/page")
    @PreAuthorize("hasAuthority('finance:tax:config')")
    public Result<Object> taxPage(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(taxConfigService.selectPage(page, size));
    }

    /** 新增税率配置，配置字段由请求体中的 TaxConfig 提供。 */
    @PostMapping("/tax")
    @PreAuthorize("hasAuthority('finance:tax:config')")
    public Result<Void> addTax(@RequestBody com.mall.finance.entity.TaxConfig config) {
        taxConfigService.save(config);
        return Result.success(null);
    }

    /** 更新指定税率配置；路径中的 ID 为准，覆盖请求体中的 ID 后交由服务校验并保存。 */
    @PutMapping("/tax/{id}")
    @PreAuthorize("hasAuthority('finance:tax:config')")
    public Result<Void> updateTax(@PathVariable Long id,
                                   @RequestBody com.mall.finance.entity.TaxConfig config) {
        config.setId(id);
        taxConfigService.update(config);
        return Result.success(null);
    }

    /** 删除指定税率配置，实际删除规则由税率配置服务处理。 */
    @DeleteMapping("/tax/{id}")
    @PreAuthorize("hasAuthority('finance:tax:config')")
    public Result<Void> deleteTax(@PathVariable Long id) {
        taxConfigService.delete(id);
        return Result.success(null);
    }
}
