package com.rentaltech.techrental.policy.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyFileResponseDto {
    private byte[] content;
    private String filename;
    private MediaType contentType;
    private ContentDisposition contentDisposition;
    private String cacheControl;
}

