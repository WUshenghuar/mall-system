package com.mall.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiGatewayClientTest {

    @Test
    void createsAStableTraceParentForTheSameRequest() {
        AiGatewayClient client = new AiGatewayClient(new ObjectMapper());
        String traceId = (String) ReflectionTestUtils.invokeMethod(client, "traceId", "session-1", "查询订单");
        String repeatedTraceId = (String) ReflectionTestUtils.invokeMethod(client, "traceId", "session-1", "查询订单");
        String traceParent = (String) ReflectionTestUtils.invokeMethod(client, "traceParent", "session-1", "查询订单");

        assertThat(traceId).matches("[0-9a-f]{32}").isEqualTo(repeatedTraceId);
        assertThat(traceParent).isEqualTo("00-" + traceId + "-0000000000000001-01");
    }
}
