package com.rentaltech.techrental.contract.service.impl;

import com.rentaltech.techrental.contract.service.EmailService;
import com.rentaltech.techrental.contract.service.SMSService;
import com.rentaltech.techrental.contract.service.DigitalSignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.model.dto.*;
import com.rentaltech.techrental.contract.repository.ContractRepository;
import com.rentaltech.techrental.contract.service.ContractService;
import com.rentaltech.techrental.common.util.ResponseUtil;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private SMSService smsService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DigitalSignatureService digitalSignatureService;

    // ========== PIN CODE GENERATION ==========
    
    /**
     * Gửi PIN code qua SMS cho khách hàng
     */
    public ResponseEntity<?> sendSMSPIN(Long contractId, String phoneNumber) {
        try {
            // Kiểm tra hợp đồng có tồn tại và đang chờ ký không
            Optional<Contract> contractOpt = contractRepository.findById(contractId);
            if (contractOpt.isEmpty()) {
                return ResponseUtil.createErrorResponse(
                        "CONTRACT_NOT_FOUND",
                        "Không tìm thấy hợp đồng",
                        "Hợp đồng với ID " + contractId + " không tồn tại",
                        HttpStatus.NOT_FOUND
                );
            }
            
            Contract contract = contractOpt.get();
            if (contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
                return ResponseUtil.createErrorResponse(
                        "CONTRACT_NOT_READY",
                        "Hợp đồng chưa sẵn sàng để ký",
                        "Hợp đồng phải ở trạng thái 'Chờ ký'",
                        HttpStatus.BAD_REQUEST
                );
            }
            
            // Tạo PIN code 6 chữ số
            String pinCode = generatePINCode();
            
            // Gửi SMS
            boolean smsSent = smsService.sendOTP(phoneNumber, pinCode);
            if (!smsSent) {
                return ResponseUtil.createErrorResponse(
                        "SMS_SEND_FAILED",
                        "Gửi SMS thất bại",
                        "Không thể gửi mã PIN qua SMS",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
            
            // Lưu PIN code vào cache/database (có thể dùng Redis)
            savePINCode(contractId, pinCode);
            
            return ResponseUtil.createSuccessResponse(
                    "Gửi mã PIN thành công!",
                    "Mã PIN đã được gửi qua SMS đến số " + phoneNumber,
                    HttpStatus.OK
            );
            
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "SEND_PIN_FAILED",
                    "Gửi mã PIN thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Gửi PIN code qua Email cho khách hàng
     */
    public ResponseEntity<?> sendEmailPIN(Long contractId, String email) {
        try {
            // Tương tự như SMS nhưng gửi qua email
            String pinCode = generatePINCode();
            boolean emailSent = emailService.sendOTP(email, pinCode);
            
            if (!emailSent) {
                return ResponseUtil.createErrorResponse(
                        "EMAIL_SEND_FAILED",
                        "Gửi Email thất bại",
                        "Không thể gửi mã PIN qua Email",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
            
            savePINCode(contractId, pinCode);
            
            return ResponseUtil.createSuccessResponse(
                    "Gửi mã PIN thành công!",
                    "Mã PIN đã được gửi qua Email đến " + email,
                    HttpStatus.OK
            );
            
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "SEND_PIN_FAILED",
                    "Gửi mã PIN thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Tạo PIN code 6 chữ số ngẫu nhiên
     */
    private String generatePINCode() {
        Random random = new Random();
        int pin = 100000 + random.nextInt(900000); // 100000 - 999999
        return String.valueOf(pin);
    }
    
    /**
     * Lưu PIN code vào Redis với thời gian hết hạn 5 phút
     */
    private void savePINCode(Long contractId, String pinCode) {
        String key = "contract_pin_" + contractId;
        redisTemplate.opsForValue().set(key, pinCode, 300, TimeUnit.SECONDS);
    }
    
    /**
     * Xác thực PIN code
     */
    private boolean validatePINCode(Long contractId, String inputPIN) {
        // Lấy PIN từ cache/database
        String storedPIN = getStoredPINCode(contractId);
        return storedPIN != null && storedPIN.equals(inputPIN);
    }
    
    private String getStoredPINCode(Long contractId) {
        String key = "contract_pin_" + contractId;
        Object pinCode = redisTemplate.opsForValue().get(key);
        return pinCode != null ? pinCode.toString() : null;
    }

    // ========== DIGITAL SIGNATURE PROCESS ==========
    
    /**
     * Quy trình ký chữ ký điện tử hoàn chỉnh
     */
    @Override
    public DigitalSignatureResponseDto signContract(DigitalSignatureRequestDto request) {
        try {
            // Bước 1: Xác thực PIN code
            if (!validatePINCode(request.getContractId(), request.getPinCode())) {
                throw new RuntimeException("PIN code không hợp lệ hoặc đã hết hạn");
            }
            
            // Bước 2: Lấy thông tin hợp đồng
            Contract contract = contractRepository.findById(request.getContractId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng"));
            
            // Bước 3: Tạo hash của nội dung hợp đồng
            String contractHash = generateContractHash(contract);
            
            // Bước 4: Xác thực chữ ký điện tử
            boolean signatureValid = digitalSignatureService.verifySignature(
                    request.getDigitalSignature(),
                    contractHash,
                    request.getSignatureMethod()
            );
            
            if (!signatureValid) {
                throw new RuntimeException("Chữ ký điện tử không hợp lệ");
            }
            
            // Bước 5: Tạo signature hash để lưu trữ
            String signatureHash = generateSignatureHash(request.getDigitalSignature());
            
            // Bước 6: Cập nhật trạng thái hợp đồng
            contract.setStatus(ContractStatus.SIGNED);
            contract.setSignedAt(LocalDateTime.now());
            contractRepository.save(contract);
            
            // Bước 7: Lưu thông tin chữ ký
            DigitalSignatureResponseDto response = DigitalSignatureResponseDto.builder()
                    .signatureId(System.currentTimeMillis()) // Temporary ID
                    .contractId(request.getContractId())
                    .signatureHash(signatureHash)
                    .signatureMethod(request.getSignatureMethod())
                    .deviceInfo(request.getDeviceInfo())
                    .ipAddress(request.getIpAddress())
                    .signedAt(LocalDateTime.now())
                    .signatureStatus("VALID")
                    .certificateInfo("Digital Certificate Verified")
                    .auditTrail(generateAuditTrail(request))
                    .build();
            
            // Bước 8: Xóa PIN code đã sử dụng
            deletePINCode(request.getContractId());
            
            return response;
            
        } catch (Exception e) {
            throw new RuntimeException("Ký hợp đồng thất bại: " + e.getMessage());
        }
    }
    
    /**
     * Tạo hash của nội dung hợp đồng để đảm bảo tính toàn vẹn
     */
    private String generateContractHash(Contract contract) {
        try {
            String contractData = contract.getContractId() + 
                                contract.getContractNumber() + 
                                contract.getTitle() + 
                                contract.getContractContent() + 
                                contract.getCreatedAt().toString();
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contractData.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Không thể tạo hash hợp đồng");
        }
    }
    
    /**
     * Tạo hash của chữ ký để lưu trữ
     */
    private String generateSignatureHash(String digitalSignature) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(digitalSignature.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Không thể tạo hash chữ ký");
        }
    }
    
    /**
     * Tạo audit trail cho chữ ký
     */
    private String generateAuditTrail(DigitalSignatureRequestDto request) {
        return String.format(
                "Signature Method: %s, Device: %s, IP: %s, Time: %s",
                request.getSignatureMethod(),
                request.getDeviceInfo(),
                request.getIpAddress(),
                LocalDateTime.now()
        );
    }
    
    private void deletePINCode(Long contractId) {
        String key = "contract_pin_" + contractId;
        redisTemplate.delete(key);
    }

    // ========== OTHER METHODS (implement interface) ==========
    
    @Override
    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    @Override
    public Optional<Contract> getContractById(Long contractId) {
        return contractRepository.findById(contractId);
    }

    @Override
    public Optional<Contract> getContractByNumber(String contractNumber) {
        return contractRepository.findByContractNumber(contractNumber);
    }

    @Override
    public List<Contract> getContractsByCustomerId(Long customerId) {
        return contractRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Contract> getContractsByStatus(ContractStatus status) {
        return contractRepository.findByStatus(status);
    }

    @Override
    public Contract createContract(ContractCreateRequestDto request, Long createdBy) {
        try {
            // Tạo contract number tự động
            String contractNumber = generateContractNumber();
            
            // Tạo Contract entity
            Contract contract = Contract.builder()
                    .contractNumber(contractNumber)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .contractType(request.getContractType())
                    .status(ContractStatus.DRAFT)
                    .customerId(request.getCustomerId())
                    .staffId(null) // Staff ID sẽ được set sau khi có staff assignment
                    .contractContent(request.getContractContent())
                    .termsAndConditions(request.getTermsAndConditions())
                    .rentalPeriodDays(request.getRentalPeriodDays())
                    .totalAmount(request.getTotalAmount())
                    .depositAmount(request.getDepositAmount())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .expiresAt(request.getExpiresAt())
                    .createdBy(createdBy)
                    .build();
            
            // Lưu vào database
            return contractRepository.save(contract);
            
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo hợp đồng: " + e.getMessage());
        }
    }

    @Override
    public Contract updateContract(Long contractId, ContractCreateRequestDto request, Long updatedBy) {
        // Implementation
        return null;
    }

    @Override
    public void deleteContract(Long contractId) {
        // Implementation
    }

    @Override
    public Contract updateContractStatus(Long contractId, ContractStatus status) {
        try {
            Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId));
            
            contract.setStatus(status);
            contract.setUpdatedAt(LocalDateTime.now());
            
            return contractRepository.save(contract);
            
        } catch (Exception e) {
            throw new RuntimeException("Không thể cập nhật trạng thái hợp đồng: " + e.getMessage());
        }
    }

    @Override
    public Contract sendForSignature(Long contractId, Long sentBy) {
        try {
            Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId));
            
            // Kiểm tra trạng thái hợp đồng
            if (contract.getStatus() != ContractStatus.DRAFT) {
                throw new RuntimeException("Chỉ có thể gửi hợp đồng ở trạng thái DRAFT để ký");
            }
            
            // Cập nhật trạng thái thành PENDING_SIGNATURE
            contract.setStatus(ContractStatus.PENDING_SIGNATURE);
            contract.setUpdatedBy(sentBy);
            
            return contractRepository.save(contract);
            
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi hợp đồng để ký: " + e.getMessage());
        }
    }

    @Override
    public Contract cancelContract(Long contractId, String reason, Long cancelledBy) {
        // Implementation
        return null;
    }

    @Override
    public boolean verifySignature(Long contractId) {
        // Implementation
        return false;
    }

    @Override
    public DigitalSignatureResponseDto getSignatureInfo(Long contractId) {
        // Implementation
        return null;
    }

    @Override
    public List<Contract> getExpiredContracts() {
        // Implementation
        return null;
    }

    @Override
    public List<Contract> getContractsExpiringSoon(int days) {
        // Implementation
        return null;
    }

    @Override
    public Contract renewContract(Long contractId, int additionalDays) {
        // Implementation
        return null;
    }

    @Override
    public boolean validateContractForSignature(Long contractId) {
        try {
            Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId));
            
            // Kiểm tra trạng thái hợp đồng
            if (contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
                return false;
            }
            
            // Kiểm tra hợp đồng chưa hết hạn
            if (contract.getExpiresAt() != null && contract.getExpiresAt().isBefore(LocalDateTime.now())) {
                return false;
            }
            
            // Kiểm tra hợp đồng có nội dung
            if (contract.getContractContent() == null || contract.getContractContent().trim().isEmpty()) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Lỗi validate contract for signature: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean validateSignatureData(DigitalSignatureRequestDto request) {
        try {
            // Kiểm tra các trường bắt buộc
            if (request.getContractId() == null) {
                return false;
            }
            
            if (request.getDigitalSignature() == null || request.getDigitalSignature().trim().isEmpty()) {
                return false;
            }
            
            if (request.getPinCode() == null || request.getPinCode().trim().isEmpty()) {
                return false;
            }
            
            // Kiểm tra PIN code có đúng 6 chữ số
            if (!request.getPinCode().matches("\\d{6}")) {
                return false;
            }
            
            // Kiểm tra signature method hợp lệ
            if (request.getSignatureMethod() != null) {
                String method = request.getSignatureMethod();
                if (!method.equals("DIGITAL_CERTIFICATE") && 
                    !method.equals("SMS_OTP") && 
                    !method.equals("EMAIL_OTP") && 
                    !method.equals("MOBILE_APP")) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Lỗi validate signature data: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String generateContractNumber() {
        // Tạo contract number theo format: HD + YYYYMMDD + 4 digits
        LocalDateTime now = LocalDateTime.now();
        String datePrefix = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Tìm số thứ tự tiếp theo trong ngày
        List<Contract> contractsToday = contractRepository.findByContractNumberStartingWith("HD" + datePrefix);
        
        int nextNumber = contractsToday.size() + 1;
        return String.format("HD%s%04d", datePrefix, nextNumber);
    }

    @Override
    public String generateAuditTrail(Long contractId) {
        // Implementation
        return null;
    }
}
