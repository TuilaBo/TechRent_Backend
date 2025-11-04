package com.rentaltech.techrental.finance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;
import vn.payos.core.ClientOptions;

@Configuration
public class PayOSConfig {

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Bean
    public PayOS payosClient() {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("PayOS clientId is not configured");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("PayOS apiKey is not configured");
        }
        if (checksumKey == null || checksumKey.isBlank()) {
            throw new IllegalStateException("PayOS checksumKey is not configured");
        }

        return new PayOS(ClientOptions.builder()
                .clientId(clientId)
                .apiKey(apiKey)
                .checksumKey(checksumKey)
                .build());
    }
}
