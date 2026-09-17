package com.mall.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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

    @Test
    void separatesDifferentRequestIdsWithTheSameMessage() {
        AiGatewayClient client = new AiGatewayClient(new ObjectMapper());
        String first = (String) ReflectionTestUtils.invokeMethod(client, "traceId", "session-1", "查询订单", "request-1");
        String retry = (String) ReflectionTestUtils.invokeMethod(client, "traceId", "session-1", "查询订单", "request-1");
        String second = (String) ReflectionTestUtils.invokeMethod(client, "traceId", "session-1", "查询订单", "request-2");

        assertThat(first).isEqualTo(retry).isNotEqualTo(second);
    }

    @Test
    void keepsTheBudgetedPlannerCallAtTheGlmCompatibleTimeout() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread responder = Thread.startVirtualThread(() -> {
                try (Socket socket = server.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                     OutputStream output = socket.getOutputStream()) {
                    while (!reader.readLine().isEmpty()) { }
                    Thread.sleep(2_200);
                    byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                    PrintWriter headers = new PrintWriter(output, false, StandardCharsets.UTF_8);
                    headers.printf("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: %d\r\nConnection: close\r\n\r\n", body.length);
                    headers.flush();
                    output.write(body);
                    output.flush();
                } catch (Exception ignored) { }
            });

            AiGatewayClient client = new AiGatewayClient(new ObjectMapper());
            ReflectionTestUtils.setField(client, "baseUrl", "http://127.0.0.1:" + server.getLocalPort());
            ReflectionTestUtils.setField(client, "serviceToken", "test-token");

            Map<String, Object> result = client.planWithTimeout(1L, "session-1", "查询订单", List.of(), List.of(), 2_500);

            responder.join(5_000);
            assertThat(result).containsEntry("ok", true);
        }
    }
}
