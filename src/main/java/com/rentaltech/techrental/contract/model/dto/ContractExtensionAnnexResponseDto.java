package com.rentaltech.techrental.contract.model.dto;

import com.rentaltech.techrental.contract.model.ContractExtensionAnnex;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.finance.model.Invoice;
import com.rentaltech.techrental.finance.model.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractExtensionAnnexResponseDto {

    private Long annexId;
    private String annexNumber;
    private Long contractId;
    private String contractNumber;
    private Long extensionOrderId;
    private Long originalOrderId;
    private String title;
    private String description;
    private String legalReference;
    private String extensionReason;
    private LocalDateTime previousEndDate;
    private LocalDateTime extensionStartDate;
    private LocalDateTime extensionEndDate;
    private Integer extensionDays;
    private BigDecimal extensionFee;
    private BigDecimal vatRate;
    private BigDecimal vatAmount;
    private BigDecimal totalPayable;
    private BigDecimal depositAdjustment;
    private String currency;
    private String annexContent;
    private ContractStatus status;
    private LocalDateTime adminSignedAt;
    private Long adminSignedBy;
    private LocalDateTime customerSignedAt;
    private Long customerSignedBy;
    private LocalDateTime issuedAt;
    private LocalDateTime effectiveDate;
    private Long invoiceId;
    private InvoiceStatus invoiceStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ContractExtensionAnnexResponseDto from(ContractExtensionAnnex annex) {
        if (annex == null) {
            return null;
        }
        Invoice invoice = annex.getInvoice();
        return ContractExtensionAnnexResponseDto.builder()
                .annexId(annex.getAnnexId())
                .annexNumber(annex.getAnnexNumber())
                .contractId(annex.getContract() != null ? annex.getContract().getContractId() : null)
                .contractNumber(annex.getContract() != null ? annex.getContract().getContractNumber() : null)
                .extensionOrderId(annex.getExtensionOrder() != null ? annex.getExtensionOrder().getOrderId() : null)
                .originalOrderId(annex.getOriginalOrderId())
                .title(annex.getTitle())
                .description(annex.getDescription())
                .legalReference(annex.getLegalReference())
                .extensionReason(annex.getExtensionReason())
                .previousEndDate(annex.getPreviousEndDate())
                .extensionStartDate(annex.getExtensionStartDate())
                .extensionEndDate(annex.getExtensionEndDate())
                .extensionDays(annex.getExtensionDays())
                .extensionFee(annex.getExtensionFee())
                .vatRate(annex.getVatRate())
                .vatAmount(annex.getVatAmount())
                .totalPayable(annex.getTotalPayable())
                .depositAdjustment(annex.getDepositAdjustment())
                .currency(annex.getCurrency())
                .annexContent(annex.getAnnexContent())
                .status(annex.getStatus())
                .adminSignedAt(annex.getAdminSignedAt())
                .adminSignedBy(annex.getAdminSignedBy())
                .customerSignedAt(annex.getCustomerSignedAt())
                .customerSignedBy(annex.getCustomerSignedBy())
                .issuedAt(annex.getIssuedAt())
                .effectiveDate(annex.getEffectiveDate())
                .invoiceId(invoice != null ? invoice.getInvoiceId() : null)
                .invoiceStatus(invoice != null ? invoice.getInvoiceStatus() : null)
                .createdAt(annex.getCreatedAt())
                .updatedAt(annex.getUpdatedAt())
                .build();
    }
}
