package com.rentaltech.techrental.contract.model.dto;

import com.rentaltech.techrental.contract.model.ContractExtensionAnnex;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.finance.model.Invoice;
import com.rentaltech.techrental.finance.model.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Thông tin phụ lục gia hạn hợp đồng")
public class ContractExtensionAnnexResponseDto {

    @Schema(description = "ID phụ lục gia hạn")
    private Long annexId;

    @Schema(description = "Số hiệu phụ lục hiển thị cho người dùng")
    private String annexNumber;

    @Schema(description = "ID hợp đồng gốc")
    private Long contractId;

    @Schema(description = "Số hợp đồng gốc")
    private String contractNumber;

    @Schema(description = "ID bản ghi gia hạn (RentalOrderExtension)")
    private Long extensionId;

    @Schema(description = "ID đơn thuê ban đầu")
    private Long originalOrderId;

    @Schema(description = "Tiêu đề phụ lục")
    private String title;

    @Schema(description = "Mô tả nội dung phụ lục")
    private String description;

    @Schema(description = "Các căn cứ pháp lý áp dụng cho phụ lục")
    private String legalReference;

    @Schema(description = "Lý do gia hạn hợp đồng")
    private String extensionReason;

    @Schema(description = "Ngày kết thúc trước khi gia hạn")
    private LocalDateTime previousEndDate;

    @Schema(description = "Ngày bắt đầu hiệu lực gia hạn")
    private LocalDateTime extensionStartDate;

    @Schema(description = "Ngày kết thúc sau gia hạn")
    private LocalDateTime extensionEndDate;

    @Schema(description = "Số ngày gia hạn thêm")
    private Integer extensionDays;

    @Schema(description = "Phí gia hạn")
    private BigDecimal extensionFee;

    @Schema(description = "Tổng tiền khách hàng phải thanh toán")
    private BigDecimal totalPayable;

    @Schema(description = "Mức điều chỉnh tiền cọc")
    private BigDecimal depositAdjustment;

    @Schema(description = "Nội dung chi tiết của phụ lục")
    private String annexContent;

    @Schema(description = "Trạng thái xử lý phụ lục")
    private ContractStatus status;

    @Schema(description = "Thời điểm quản trị viên ký phụ lục")
    private LocalDateTime adminSignedAt;

    @Schema(description = "ID tài khoản quản trị viên ký phụ lục")
    private Long adminSignedBy;

    @Schema(description = "Thời điểm khách hàng ký phụ lục")
    private LocalDateTime customerSignedAt;

    @Schema(description = "ID tài khoản khách hàng ký phụ lục")
    private Long customerSignedBy;

    @Schema(description = "Ngày phát hành phụ lục")
    private LocalDateTime issuedAt;

    @Schema(description = "Ngày hiệu lực của phụ lục")
    private LocalDateTime effectiveDate;

    @Schema(description = "ID hóa đơn liên kết (nếu có)")
    private Long invoiceId;

    @Schema(description = "Trạng thái hóa đơn liên quan")
    private InvoiceStatus invoiceStatus;

    @Schema(description = "Ngày tạo bản ghi phụ lục")
    private LocalDateTime createdAt;

    @Schema(description = "Ngày cập nhật bản ghi phụ lục gần nhất")
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
                .extensionId(annex.getRentalOrderExtension() != null ? annex.getRentalOrderExtension().getExtensionId() : null)
                .originalOrderId(annex.getOriginalOrderId())
                .title(annex.getTitle())
                .description(annex.getDescription())
                .legalReference(annex.getLegalReference())
                .previousEndDate(annex.getPreviousEndDate())
                .extensionStartDate(annex.getExtensionStartDate())
                .extensionEndDate(annex.getExtensionEndDate())
                .extensionDays(annex.getExtensionDays())
                .extensionFee(annex.getExtensionFee())
                .totalPayable(annex.getTotalPayable())
                .depositAdjustment(annex.getDepositAdjustment())
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
