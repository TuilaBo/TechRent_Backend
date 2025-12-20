package com.rentaltech.techrental.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Allow requests from your frontend using patterns
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedOriginPattern("https://localhost:*");
        config.addAllowedOriginPattern("http://127.0.0.1:*");
        config.addAllowedOriginPattern("https://127.0.0.1:*");
        config.addAllowedOriginPattern("null");  // Cho phép file:// protocol
        config.addAllowedOriginPattern("*");  // Cho phép tất cả origins (development only)

        // Allow common HTTP methods
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("PATCH");

        // Allow common headers
        config.addAllowedHeader("Origin");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Accept-Charset");
        config.addAllowedHeader("Accept-Encoding");
        
        // Set exposed headers for UTF-8
        config.addExposedHeader("Content-Type");
        config.addExposedHeader("Accept-Charset");

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
