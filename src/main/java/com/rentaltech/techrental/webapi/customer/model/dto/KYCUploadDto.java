package com.rentaltech.techrental.webapi.customer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KYCUploadDto {
    private String imageUrl; // URL của ảnh đã upload
    private String imageType; // "front_cccd", "back_cccd", "selfie"
}


