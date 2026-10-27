package com.mall.ai.controller;

import com.mall.ai.dto.AiKnowledgeDocument;
import com.mall.ai.service.AiKnowledgeService;
import com.mall.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 提供受权限保护的 AI 知识库条目查询和维护入口。 */
/**
 * AI知识库控制器
 * 提供知识文档的列表查询和保存功能
 */
@RestController
@RequestMapping("/api/ai/knowledge")
@RequiredArgsConstructor
public class AiKnowledgeController {
    private final AiKnowledgeService knowledgeService;

    /**
     * 获取知识文档列表
     * @return 返回知识文档列表的Result对象
     * 需要order:support:list权限
     */
    @GetMapping
    @PreAuthorize("hasAuthority('order:support:list')")
    public Result<List<AiKnowledgeDocument>> list() {
        return Result.success(knowledgeService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('order:support:handle')")
    public Result<AiKnowledgeDocument> save(@Valid @RequestBody AiKnowledgeDocument document) {
        return Result.success(knowledgeService.save(document));
    }
}
