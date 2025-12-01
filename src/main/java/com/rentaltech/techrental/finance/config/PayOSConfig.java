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
            throw new IllegalStateException("Chưa cấu hình clientId cho PayOS");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình apiKey cho PayOS");
        }
        if (checksumKey == null || checksumKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình checksumKey cho PayOS");
        }

        return new PayOS(ClientOptions.builder()
                .clientId(clientId)
                .apiKey(apiKey)
                .checksumKey(checksumKey)
                .build());
    }
}
