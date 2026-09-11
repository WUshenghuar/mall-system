package com.mall.trade.service.impl;

import com.mall.trade.entity.TradeOrder;
import com.mall.trade.entity.TradePay;
import com.mall.trade.mapper.TradePayMapper;
import com.mall.trade.service.TradeOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayServiceImplTest {

    @Test
    void createPayReusesPendingPaymentAfterLockingOwnedOrder() {
        TradePayMapper payMapper = mock(TradePayMapper.class);
        TradeOrderService orderService = mock(TradeOrderService.class);
        TradeOrder order = new TradeOrder(); order.setOrderNo("T-1"); order.setUserId(9L); order.setOrderStatus(0);
        order.setPayAmount(new BigDecimal("19.90"));
        TradePay existing = new TradePay(); existing.setPayNo("P-1"); existing.setPayStatus(0);
        when(orderService.getOwnedByOrderNoForUpdate("T-1", 9L)).thenReturn(order);
        when(payMapper.selectOne(any())).thenReturn(existing);

        TradePay result = new PayServiceImpl(payMapper, orderService).createPay("T-1", 1, 9L);

        assertThat(result).isSameAs(existing);
        verify(orderService).getOwnedByOrderNoForUpdate("T-1", 9L);
    }

    @Test
    void createPayRejectsUnknownPaymentType() {
        PayServiceImpl service = new PayServiceImpl(mock(TradePayMapper.class), mock(TradeOrderService.class));

        assertThatThrownBy(() -> service.createPay("T-1", 9, 9L))
                .hasMessage("支付方式不支持");
    }

    @Test
    void repeatedAlipaySuccessCallbacksAreProcessedOnlyOnce() throws Exception {
        TradePayMapper payMapper = mock(TradePayMapper.class);
        TradeOrderService orderService = mock(TradeOrderService.class);
        TradePay pay = new TradePay();
        pay.setId(1L);
        pay.setOrderNo("T-1");
        pay.setPayType(1);
        pay.setPayStatus(0);
        pay.setPayAmount(new BigDecimal("100.00"));
        TradeOrder order = new TradeOrder();
        order.setOrderNo("T-1");
        order.setPayAmount(new BigDecimal("100.00"));
        when(payMapper.selectOne(any())).thenReturn(pay);
        when(orderService.getByOrderNo("T-1")).thenReturn(order);
        when(orderService.markPaid("T-1", 1)).thenReturn(true);
        doAnswer(invocation -> {
            pay.setPayStatus(1);
            return 1;
        }).when(payMapper).markSuccess(eq(1L), anyString());

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        PayServiceImpl service = new PayServiceImpl(payMapper, orderService);
        ReflectionTestUtils.setField(service, "alipayPublicKey",
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        Map<String, String> callback = signedCallback(keyPair, "100.00");

        assertThat(service.handleAlipayNotify(callback)).isTrue();
        assertThat(service.handleAlipayNotify(callback)).isTrue();

        verify(payMapper, times(1)).markSuccess(eq(1L), anyString());
        verify(orderService, times(1)).markPaid("T-1", 1);
    }

    private Map<String, String> signedCallback(KeyPair keyPair, String amount) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("out_trade_no", "T-1");
        params.put("total_amount", amount);
        params.put("trade_status", "TRADE_SUCCESS");
        String content = params.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining("&"));
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(content.getBytes(StandardCharsets.UTF_8));
        params.put("sign_type", "RSA2");
        params.put("sign", Base64.getEncoder().encodeToString(signer.sign()));
        return params;
    }
}
