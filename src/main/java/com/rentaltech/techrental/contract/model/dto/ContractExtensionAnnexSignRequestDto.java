package com.rentaltech.techrental.contract.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractExtensionAnnexSignRequestDto {

    @NotBlank
    private String digitalSignature;

    @NotBlank
    private String signatureMethod;

    private String deviceInfo;

    private String ipAddress;

    private String pinCode;
}
