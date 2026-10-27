package com.mall.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI知识文档实体类
 * 使用@Data注解自动生成getter、setter、toString等方法
 */
@Data
public class AiKnowledgeDocument {
    /**
     * 文档ID
     * 限制最大长度为64个字符
     */
    @Size(max = 64)
    private String id;

    /**
     * 文档标题
     * 不能为空，最大长度限制为120个字符
     */
    @NotBlank
    @Size(max = 120)
    private String title;

    /**
     * 文档分类
     * 不能为空，最大长度限制为64个字符
     */
    @NotBlank
    @Size(max = 64)
    private String category;

    /**
     * 文档内容
     * 不能为空，最大长度限制为5000个字符
     */
    @NotBlank
    @Size(max = 5000)
    private String content;

    /**
     * 文档是否启用
     * 默认值为true
     */
    private Boolean enabled = true;
}
