package com.rentaltech.techrental.contract.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class SMSServiceTest {

    private SMSService smsService;

    @BeforeEach
    void setUp() {
        smsService = new SMSService();
        ReflectionTestUtils.setField(smsService, "smsApiKey", "token");
    }

    @Test
    void sendOtpReturnsTrueWhenGatewayRespondsSuccess() {
        stubWebClient(ClientResponse.create(HttpStatus.OK)
                .body("{\"success\":true}")
                .build());

        boolean result = smsService.sendOTP("0901234567", "123456");

        assertThat(result).isTrue();
    }

    @Test
    void sendOtpReturnsFalseWhenGatewayErrors() {
        ExchangeFunction failingExchange = clientRequest -> Mono.error(new RuntimeException("network"));
        WebClient client = WebClient.builder().exchangeFunction(failingExchange).build();
        ReflectionTestUtils.setField(smsService, "webClient", client);

        boolean result = smsService.sendOTP("0901234567", "123456");

        assertThat(result).isFalse();
    }

    @Test
    void sendOtpViaTwilioStubReturnsTrue() {
        assertThat(smsService.sendOTPViaTwilio("0901234567", "123456")).isTrue();
    }

    @Test
    void sendOtpViaLocalGatewayReturnsTrue() {
        assertThat(smsService.sendOTPViaLocalGateway("0901234567", "123456")).isTrue();
    }

    private void stubWebClient(ClientResponse response) {
        ExchangeFunction exchangeFunction = clientRequest -> Mono.just(response);
        WebClient client = WebClient.builder().exchangeFunction(exchangeFunction).build();
        ReflectionTestUtils.setField(smsService, "webClient", client);
    }
}
