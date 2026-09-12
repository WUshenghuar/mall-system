package com.mall.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiKnowledgeDocument {
    @Size(max = 64)
    private String id;

    @NotBlank
    @Size(max = 120)
    private String title;

    @NotBlank
    @Size(max = 64)
    private String category;

    @NotBlank
    @Size(max = 5000)
    private String content;
}
