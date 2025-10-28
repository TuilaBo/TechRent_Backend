package com.rentaltech.techrental.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(
            @Value("${CLOUDINARY_URL:}") String cloudinaryUrl,
            @Value("${spring.cloudinary.url:}") String cloudinaryUrlAlt
    ) {
        String url = (cloudinaryUrlAlt != null && !cloudinaryUrlAlt.isBlank()) ? cloudinaryUrlAlt : cloudinaryUrl;
        if (url == null || url.isBlank()) {
            String envUrl = System.getenv("CLOUDINARY_URL");
            if (envUrl != null && !envUrl.isBlank()) {
                url = envUrl;
            }
        }
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("CLOUDINARY_URL is not configured (set CLOUDINARY_URL env or spring.cloudinary.url in properties)");
        }
        return new Cloudinary(url);
    }
}


