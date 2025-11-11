package com.rentaltech.techrental.finance.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class VnpayConfig {

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.url}")
    private String url;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Value("${vnpay.ipn-url}")
    private String ipnUrl;

    @Value("${vnpay.frontend-success-url:http://localhost:5173/success}")
    private String frontendSuccessUrl;

    @Value("${vnpay.frontend-failure-url:http://localhost:5173/failure}")
    private String frontendFailureUrl;
}

