package com.mall.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiFeedbackRequest {
    @NotNull
    private Long messageId;

    @NotNull
    @Min(-1)
    @Max(1)
    private Integer feedback;
}
