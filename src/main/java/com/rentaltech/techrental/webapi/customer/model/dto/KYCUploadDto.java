package com.rentaltech.techrental.webapi.customer.model.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KYCUploadDto {
    private String imageUrl; // URL của ảnh đã upload
    private String imageType; // "front_cccd", "back_cccd", "selfie"
}


