package com.mall.ai.service;

import com.mall.ai.dto.AiKnowledgeDocument;
import com.mall.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiKnowledgeService {
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private final AiGatewayClient gatewayClient;

    public List<AiKnowledgeDocument> list() {
        return gatewayClient.listKnowledge();
    }

    public AiKnowledgeDocument save(AiKnowledgeDocument document) {
        String id = StringUtils.hasText(document.getId()) ? document.getId().trim()
                : "kb-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        if (!ID.matcher(id).matches()) throw new BusinessException("知识条目标识只能包含字母、数字、下划线和短横线");
        document.setId(id);
        document.setTitle(document.getTitle().trim());
        document.setCategory(document.getCategory().trim());
        document.setContent(document.getContent().trim());
        document.setEnabled(!Boolean.FALSE.equals(document.getEnabled()));
        return gatewayClient.saveKnowledge(document);
    }
}
