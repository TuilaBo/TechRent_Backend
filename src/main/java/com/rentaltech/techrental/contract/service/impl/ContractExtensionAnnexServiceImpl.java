package com.rentaltech.techrental.contract.service.impl;

import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractExtensionAnnex;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.model.dto.ContractExtensionAnnexResponseDto;
import com.rentaltech.techrental.contract.model.dto.ContractExtensionAnnexSignRequestDto;
import com.rentaltech.techrental.contract.repository.ContractExtensionAnnexRepository;
import com.rentaltech.techrental.contract.service.ContractExtensionAnnexService;
import com.rentaltech.techrental.contract.service.EmailService;
import com.rentaltech.techrental.contract.service.DigitalSignatureService;
import com.rentaltech.techrental.finance.model.Invoice;
import com.rentaltech.techrental.finance.model.InvoiceStatus;
import com.rentaltech.techrental.finance.model.InvoiceType;
import com.rentaltech.techrental.finance.model.PaymentMethod;
import com.rentaltech.techrental.finance.repository.InvoiceRepository;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.model.RentalOrderExtension;
import com.rentaltech.techrental.rentalorder.repository.OrderDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractExtensionAnnexServiceImpl implements ContractExtensionAnnexService {

    private static final long PIN_TTL_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private final ContractExtensionAnnexRepository annexRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final DigitalSignatureService digitalSignatureService;
    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;

    private final ConcurrentHashMap<Long, PinCacheEntry> pinCache = new ConcurrentHashMap<>();

    @Override
    public ContractExtensionAnnex createAnnexForExtension(Contract contract,
                                                          RentalOrderExtension rentalOrderExtension,
                                                          Long createdBy) {
        if (contract == null || rentalOrderExtension == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu để tạo phụ lục gia hạn");
        }
        RentalOrder originalOrder = rentalOrderExtension.getRentalOrder();

        long annexCount = annexRepository.countByContract_ContractId(contract.getContractId());
        String annexNumber = buildAnnexNumber(contract.getContractNumber(), annexCount + 1);
        List<OrderDetail> extensionDetails = originalOrder != null
                ? orderDetailRepository.findByRentalOrder_OrderId(originalOrder.getOrderId())
                : List.of();

        int extensionDays = rentalOrderExtension.getDurationDays() != null ? rentalOrderExtension.getDurationDays() : 0;
        BigDecimal extensionFee = defaultZero(rentalOrderExtension.getAdditionalPrice());
        BigDecimal vatRate = BigDecimal.ZERO;
        BigDecimal vatAmount = extensionFee.multiply(vatRate);
        BigDecimal totalPayable = extensionFee.add(vatAmount);

        String annexContent = buildAnnexContent(contract, originalOrder, rentalOrderExtension, extensionDetails, extensionDays, extensionFee, vatAmount, totalPayable);

        ContractExtensionAnnex annex = ContractExtensionAnnex.builder()
                .annexNumber(annexNumber)
                .contract(contract)
                .rentalOrderExtension(rentalOrderExtension)
                .originalOrderId(originalOrder.getOrderId())
                .title("Phụ lục gia hạn hợp đồng " + contract.getContractNumber())
                .description("Gia hạn hợp đồng thuê thiết bị đến ngày " + rentalOrderExtension.getExtensionEnd())
                .legalReference("Căn cứ Bộ luật Dân sự 2015 và Luật Thương mại 2005")
                .extensionReason("Nhu cầu tiếp tục sử dụng thiết bị của khách hàng")
                .previousEndDate(originalOrder.getEffectiveEndDate())
                .extensionStartDate(rentalOrderExtension.getExtensionStart())
                .extensionEndDate(rentalOrderExtension.getExtensionEnd())
                .extensionDays(extensionDays)
                .extensionFee(extensionFee)
                .vatRate(vatRate)
                .vatAmount(vatAmount)
                .totalPayable(totalPayable)
                .depositAdjustment(BigDecimal.ZERO)
                .annexContent(annexContent)
                .status(ContractStatus.PENDING_ADMIN_SIGNATURE)
                .issuedAt(LocalDateTime.now())
                .effectiveDate(rentalOrderExtension.getExtensionStart())
                .createdBy(createdBy)
                .build();

        return annexRepository.save(annex);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractExtensionAnnexResponseDto> getAnnexesForContract(Long contractId) {
        return annexRepository.findByContract_ContractId(contractId).stream()
                .map(ContractExtensionAnnexResponseDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractExtensionAnnexResponseDto getAnnexDetail(Long contractId, Long annexId) {
        ContractExtensionAnnex annex = getAnnexOrThrow(contractId, annexId);
        return ContractExtensionAnnexResponseDto.from(annex);
    }

    @Override
    public ContractExtensionAnnexResponseDto signAsAdmin(Long contractId,
                                                         Long annexId,
                                                         Long adminAccountId,
                                                         ContractExtensionAnnexSignRequestDto request) {
        ContractExtensionAnnex annex = getAnnexOrThrow(contractId, annexId);
        if (annex.getStatus() != ContractStatus.PENDING_ADMIN_SIGNATURE) {
            throw new IllegalStateException("Phụ lục không ở trạng thái chờ admin ký");
        }
        verifySignature(request, annex);

        LocalDateTime now = LocalDateTime.now();
        annex.setAdminSignedAt(now);
        annex.setAdminSignedBy(adminAccountId);
        annex.setAdminSignatureHash(hashSignature(request.getDigitalSignature()));
        annex.setStatus(ContractStatus.PENDING_SIGNATURE);
        annex.setUpdatedBy(adminAccountId);

        ContractExtensionAnnex saved = annexRepository.save(annex);
        return ContractExtensionAnnexResponseDto.from(saved);
    }

    @Override
    public ContractExtensionAnnexResponseDto signAsCustomer(Long contractId,
                                                            Long annexId,
                                                            Long customerAccountId,
                                                            ContractExtensionAnnexSignRequestDto request) {
        ContractExtensionAnnex annex = getAnnexOrThrow(contractId, annexId);
        if (annex.getStatus() != ContractStatus.PENDING_SIGNATURE) {
            throw new IllegalStateException("Phụ lục không ở trạng thái chờ khách hàng ký");
        }
        if (annex.getAdminSignedAt() == null) {
            throw new IllegalStateException("Admin chưa ký phụ lục");
        }
        if (!StringUtils.hasText(request.getPinCode())) {
            throw new IllegalArgumentException("Cần cung cấp mã PIN để ký phụ lục");
        }
        if (!validatePin(annex.getAnnexId(), request.getPinCode())) {
            throw new IllegalStateException("Mã PIN không hợp lệ hoặc đã hết hạn");
        }
        verifySignature(request, annex);

        LocalDateTime now = LocalDateTime.now();
        annex.setCustomerSignedAt(now);
        annex.setCustomerSignedBy(customerAccountId);
        annex.setCustomerSignatureHash(hashSignature(request.getDigitalSignature()));
        annex.setStatus(ContractStatus.ACTIVE);
        annex.setUpdatedBy(customerAccountId);

        if (annex.getInvoice() == null) {
            Invoice invoice = createInvoiceForAnnex(annex);
            annex.setInvoice(invoice);
        }

        ContractExtensionAnnex saved = annexRepository.save(annex);
        clearPin(annex.getAnnexId());
        return ContractExtensionAnnexResponseDto.from(saved);
    }

    @Override
    public void sendSignaturePinByEmail(Long contractId, Long annexId, String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        ContractExtensionAnnex annex = getAnnexOrThrow(contractId, annexId);
        String pin = generatePin();
        boolean sent = emailService.sendOTP(email, pin);
        if (!sent) {
            throw new IllegalStateException("Không thể gửi mã PIN qua email");
        }
        savePin(annex.getAnnexId(), pin);
    }

    private ContractExtensionAnnex getAnnexOrThrow(Long contractId, Long annexId) {
        ContractExtensionAnnex annex = annexRepository.findById(annexId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phụ lục với id: " + annexId));
        if (annex.getContract() == null || !Objects.equals(annex.getContract().getContractId(), contractId)) {
            throw new IllegalArgumentException("Phụ lục không thuộc hợp đồng " + contractId);
        }
        return annex;
    }

    private String buildAnnexNumber(String contractNumber, long sequence) {
        return contractNumber + "-PL-" + String.format("%02d", sequence);
    }

    private String buildAnnexContent(Contract contract,
                                     RentalOrder originalOrder,
                                     RentalOrderExtension rentalOrderExtension,
                                     List<OrderDetail> details,
                                     int extensionDays,
                                     BigDecimal extensionFee,
                                     BigDecimal vatAmount,
                                     BigDecimal total) {
        StringBuilder builder = new StringBuilder();
        builder.append("PHỤ LỤC GIA HẠN HỢP ĐỒNG THUÊ THIẾT BỊ\n");
        builder.append("Số phụ lục: ").append(contract.getContractNumber()).append("\n\n");
        builder.append("Căn cứ hợp đồng số: ").append(contract.getContractNumber()).append(" ký ngày ")
                .append(contract.getStartDate()).append("; hai bên thống nhất gia hạn như sau:\n");
        builder.append("- Thời hạn gia hạn: từ ").append(rentalOrderExtension.getExtensionStart()).append(" đến ")
                .append(rentalOrderExtension.getExtensionEnd()).append(" (" + extensionDays + " ngày).\n");
        builder.append("- Giá trị gia hạn: ").append(extensionFee).append(" VND, thuế VAT: ")
                .append(vatAmount).append(" VND, tổng thanh toán: ").append(total).append(" VND.\n");
        builder.append("- Thiết bị áp dụng: \n");
        for (OrderDetail detail : details) {
            builder.append("  + ")
                    .append(detail.getQuantity()).append("x ")
                    .append(detail.getDeviceModel().getDeviceName())
                    .append(" - Đơn giá/ngày: ")
                    .append(detail.getPricePerDay())
                    .append(" VND\n");
        }
        builder.append("Các điều khoản khác của hợp đồng gốc giữ nguyên hiệu lực.\n");
        builder.append("Phụ lục là bộ phận không tách rời của hợp đồng và tuân thủ pháp luật Việt Nam.");
        return builder.toString();
    }

    private void verifySignature(ContractExtensionAnnexSignRequestDto request, ContractExtensionAnnex annex) {
        if (request == null || !StringUtils.hasText(request.getDigitalSignature())) {
            throw new IllegalArgumentException("Thiếu dữ liệu chữ ký");
        }
        String method = StringUtils.hasText(request.getSignatureMethod())
                ? request.getSignatureMethod()
                : "ANNEX_SIGNATURE";
        boolean valid = digitalSignatureService.verifySignature(request.getDigitalSignature(), buildAnnexHash(annex), method);
        if (!valid) {
            throw new IllegalStateException("Chữ ký điện tử không hợp lệ");
        }
    }

    private String buildAnnexHash(ContractExtensionAnnex annex) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = annex.getAnnexNumber() +
                    annex.getContract().getContractNumber() +
                    annex.getAnnexContent() +
                    annex.getExtensionStartDate();
            byte[] hash = digest.digest(payload.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không thể tạo hash phụ lục", e);
        }
    }

    private String hashSignature(String signature) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(signature.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không thể lưu chữ ký", e);
        }
    }

    private Invoice createInvoiceForAnnex(ContractExtensionAnnex annex) {
        RentalOrder originalOrder = annex.getRentalOrderExtension() != null ? annex.getRentalOrderExtension().getRentalOrder() : null;
        BigDecimal subTotal = defaultZero(annex.getExtensionFee());
        BigDecimal taxAmount = defaultZero(annex.getVatAmount());
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal depositApplied = defaultZero(annex.getDepositAdjustment());
        BigDecimal totalAmount = defaultZero(annex.getTotalPayable());

        Invoice invoice = Invoice.builder()
                .rentalOrder(originalOrder)
                .invoiceType(InvoiceType.RENT_PAYMENT)
                .paymentMethod(PaymentMethod.PAYOS)
                .paymentDate(null)
                .subTotal(subTotal)
                .taxAmount(taxAmount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .depositApplied(depositApplied)
                .dueDate(LocalDateTime.now().plusDays(3))
                .invoiceStatus(InvoiceStatus.PROCESSING)
                .proofUrl(null)
                .issueDate(LocalDateTime.now())
                .build();
        return invoiceRepository.save(invoice);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String generatePin() {
        Random random = new Random();
        int pin = 100000 + random.nextInt(900000);
        return String.valueOf(pin);
    }

    private void savePin(Long annexId, String pin) {
        long expiry = System.currentTimeMillis() + PIN_TTL_MILLIS;
        pinCache.put(annexId, new PinCacheEntry(pin, expiry));
    }

    private boolean validatePin(Long annexId, String inputPin) {
        PinCacheEntry entry = pinCache.get(annexId);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            pinCache.remove(annexId);
            return false;
        }
        return entry.pin.equals(inputPin);
    }

    private void clearPin(Long annexId) {
        pinCache.remove(annexId);
    }

    private static class PinCacheEntry {
        private final String pin;
        private final long expiryTime;

        private PinCacheEntry(String pin, long expiryTime) {
            this.pin = pin;
            this.expiryTime = expiryTime;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
