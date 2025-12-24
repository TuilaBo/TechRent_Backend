package com.rentaltech.techrental.contract.service.impl;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.model.ContractType;
import com.rentaltech.techrental.contract.model.DeviceContractTerm;
import com.rentaltech.techrental.contract.model.dto.ContractCreateRequestDto;
import com.rentaltech.techrental.contract.model.dto.DigitalSignatureRequestDto;
import com.rentaltech.techrental.contract.model.dto.DigitalSignatureResponseDto;
import com.rentaltech.techrental.contract.repository.ContractRepository;
import com.rentaltech.techrental.contract.service.*;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.OrderDetailRepository;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractServiceImpl.class);

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private SMSService smsService;

    @Autowired
    private EmailService emailService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DigitalSignatureService digitalSignatureService;

    // Fallback in-memory cache for PIN codes
    private final ConcurrentHashMap<String, PinCacheEntry> pinCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);
    
    @Autowired
    private RentalOrderRepository rentalOrderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private DeviceContractTermService deviceContractTermService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CustomerRepository customerRepository;

    public ContractServiceImpl() {
        // Cleanup expired PIN codes every minute
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredPins, 1, 1, TimeUnit.MINUTES);
    }

    // Inner class for PIN cache entry
    private static class PinCacheEntry {
        String pin;
        long expiryTime;
        
        PinCacheEntry(String pin, long expiryTime) {
            this.pin = pin;
            this.expiryTime = expiryTime;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
    
    private void cleanupExpiredPins() {
        pinCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

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
     * Lưu PIN code vào Redis hoặc in-memory cache với thời gian hết hạn 5 phút
     */
    private void savePINCode(Long contractId, String pinCode) {
        String key = "contract_pin_" + contractId;
        
        try {
            // Try Redis first
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, pinCode, 300, TimeUnit.SECONDS);
            } else {
                // Fallback to in-memory cache
                long expiryTime = System.currentTimeMillis() + (300 * 1000); // 5 minutes
                pinCache.put(key, new PinCacheEntry(pinCode, expiryTime));
            }
        } catch (Exception e) {
            // If Redis fails, use in-memory cache
            long expiryTime = System.currentTimeMillis() + (300 * 1000);
            pinCache.put(key, new PinCacheEntry(pinCode, expiryTime));
        }
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
        
        try {
            // Try Redis first
            if (redisTemplate != null) {
                Object pinCode = redisTemplate.opsForValue().get(key);
                if (pinCode != null) {
                    return pinCode.toString();
                }
            }
        } catch (Exception e) {
            // Redis not available, fallback
        }
        
        // Fallback to in-memory cache
        PinCacheEntry entry = pinCache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.pin;
        } else if (entry != null) {
            // Remove expired entry
            pinCache.remove(key);
        }
        
        return null;
    }

    // ========== DIGITAL SIGNATURE PROCESS ==========
    
    /**
     * Quy trình ký chữ ký điện tử hoàn chỉnh (cho Customer)
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
            
            // Kiểm tra hợp đồng phải ở trạng thái PENDING_SIGNATURE (chờ customer ký)
            if (contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
                throw new RuntimeException("Hợp đồng chưa sẵn sàng để khách hàng ký. Trạng thái hiện tại: " + contract.getStatus());
            }
            
            // Kiểm tra admin đã ký chưa
            if (contract.getAdminSignedAt() == null || contract.getAdminSignedBy() == null) {
                throw new RuntimeException("Admin chưa ký hợp đồng. Vui lòng đợi admin ký trước.");
            }
            
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
            
            // Bước 6: Lưu thông tin customer ký
            LocalDateTime now = LocalDateTime.now();
            contract.setCustomerSignedAt(now);
            contract.setCustomerSignedBy(contract.getCustomerId()); // Customer ID từ contract
            contract.setSignedAt(now); // Thời gian ký cuối cùng
            
            // Bước 7: Cập nhật trạng thái hợp đồng thành ACTIVE khi cả 2 đã ký
            contract.setStatus(ContractStatus.ACTIVE);
            contractRepository.save(contract);
            
            // Bước 8: Lưu thông tin chữ ký
            DigitalSignatureResponseDto response = DigitalSignatureResponseDto.builder()
                    .signatureId(System.currentTimeMillis()) // Temporary ID
                    .contractId(request.getContractId())
                    .signatureHash(signatureHash)
                    .signatureMethod(request.getSignatureMethod())
                    .deviceInfo(request.getDeviceInfo())
                    .ipAddress(request.getIpAddress())
                    .signedAt(now)
                    .signatureStatus("VALID")
                    .certificateInfo("Chứng thư số đã được xác thực")
                    .auditTrail(generateAuditTrail(request))
                    .build();
            
            // Bước 9: Xóa PIN code đã sử dụng
            deletePINCode(request.getContractId());
            
            return response;
            
        } catch (Exception e) {
            throw new RuntimeException("Ký hợp đồng thất bại: " + e.getMessage());
        }
    }
    
    /**
     * Admin ký hợp đồng trước
     */
    @Override
    public DigitalSignatureResponseDto signContractByAdmin(Long contractId, Long adminId, DigitalSignatureRequestDto request) {
        try {
            // Bước 1: Lấy thông tin hợp đồng
            Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng"));
            
            // Kiểm tra hợp đồng phải ở trạng thái PENDING_ADMIN_SIGNATURE
            if (contract.getStatus() != ContractStatus.PENDING_ADMIN_SIGNATURE) {
                throw new RuntimeException("Hợp đồng không ở trạng thái chờ admin ký. Trạng thái hiện tại: " + contract.getStatus());
            }
            
            // Bước 2: Tạo hash của nội dung hợp đồng
            String contractHash = generateContractHash(contract);
            
            // Bước 3: Xác thực chữ ký điện tử (admin có thể không cần PIN)
            boolean signatureValid = digitalSignatureService.verifySignature(
                    request.getDigitalSignature(),
                    contractHash,
                    request.getSignatureMethod() != null ? request.getSignatureMethod() : "ADMIN_SIGNATURE"
            );
            
            if (!signatureValid) {
                throw new RuntimeException("Chữ ký điện tử không hợp lệ");
            }
            
            // Bước 4: Tạo signature hash để lưu trữ
            String signatureHash = generateSignatureHash(request.getDigitalSignature());
            
            // Bước 5: Lưu thông tin admin ký
            LocalDateTime now = LocalDateTime.now();
            contract.setAdminSignedAt(now);
            contract.setAdminSignedBy(adminId);
            
            // Bước 6: Chuyển sang trạng thái chờ customer ký
            contract.setStatus(ContractStatus.PENDING_SIGNATURE);
            Contract savedContract = contractRepository.save(contract);
            notifyCustomerContractReadyForSignature(savedContract);
            
            // Bước 7: Lưu thông tin chữ ký
            DigitalSignatureResponseDto response = DigitalSignatureResponseDto.builder()
                    .signatureId(System.currentTimeMillis())
                    .contractId(contractId)
                    .signatureHash(signatureHash)
                    .signatureMethod(request.getSignatureMethod() != null ? request.getSignatureMethod() : "ADMIN_SIGNATURE")
                    .deviceInfo(request.getDeviceInfo())
                    .ipAddress(request.getIpAddress())
                    .signedAt(now)
                    .signatureStatus("VALID")
                    .certificateInfo("Chữ ký admin đã được xác thực")
                    .auditTrail(generateAuditTrail(request))
                    .build();
            
            return response;

        } catch (Exception e) {
            throw new RuntimeException("Admin ký hợp đồng thất bại: " + e.getMessage());
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
                "Phương thức ký: %s, Thiết bị: %s, IP: %s, Thời gian: %s",
                request.getSignatureMethod(),
                request.getDeviceInfo(),
                request.getIpAddress(),
                LocalDateTime.now()
        );
    }
    
    private void deletePINCode(Long contractId) {
        String key = "contract_pin_" + contractId;
        
        try {
            if (redisTemplate != null) {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            // Ignore Redis errors
        }
        
        // Also remove from in-memory cache
        pinCache.remove(key);
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
    public List<Contract> getContractsByCustomerIdAndStatus(Long customerId, ContractStatus status) {
        return contractRepository.findByStatusAndCustomerId(status, customerId);
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
                    .orderId(request.getOrderId()) // Link to rental order if provided
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

            if (request.getOrderId() != null) {
                RentalOrder linkedOrder = rentalOrderRepository.findById(request.getOrderId()).orElse(null);
                if (linkedOrder != null) {
                    List<OrderDetail> orderDetails = orderDetailRepository.findByRentalOrder_OrderId(request.getOrderId());
                    String enrichedTerms = appendDeviceTerms(contract.getTermsAndConditions(), linkedOrder, orderDetails);
                    contract.setTermsAndConditions(enrichedTerms);
                }
            }
            
            // Lưu vào database
            return contractRepository.save(contract);
            
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo hợp đồng: " + e.getMessage());
        }
    }

    @Override
    public Contract updateContract(Long contractId, ContractCreateRequestDto request, Long updatedBy) {
        try {
            // Lấy hợp đồng hiện tại
            Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId));
            
            // Chỉ cho phép update khi contract ở trạng thái DRAFT
            if (contract.getStatus() != ContractStatus.DRAFT) {
                throw new RuntimeException(
                    "Chỉ có thể chỉnh sửa hợp đồng ở trạng thái DRAFT. " +
                    "Trạng thái hiện tại: " + contract.getStatus().getDisplayName() + 
                    ". Không thể chỉnh sửa hợp đồng đã bắt đầu quy trình ký hoặc đã ký."
                );
            }
            
            // Cập nhật các field từ request
            if (request.getTitle() != null) {
                contract.setTitle(request.getTitle());
            }
            if (request.getDescription() != null) {
                contract.setDescription(request.getDescription());
            }
            if (request.getContractType() != null) {
                contract.setContractType(request.getContractType());
            }
            if (request.getContractContent() != null) {
                contract.setContractContent(request.getContractContent());
            }
            if (request.getTermsAndConditions() != null) {
                contract.setTermsAndConditions(request.getTermsAndConditions());
            }
            if (request.getRentalPeriodDays() != null) {
                contract.setRentalPeriodDays(request.getRentalPeriodDays());
            }
            if (request.getTotalAmount() != null) {
                contract.setTotalAmount(request.getTotalAmount());
            }
            if (request.getDepositAmount() != null) {
                contract.setDepositAmount(request.getDepositAmount());
            }
            if (request.getStartDate() != null) {
                contract.setStartDate(request.getStartDate());
            }
            if (request.getEndDate() != null) {
                contract.setEndDate(request.getEndDate());
            }
            if (request.getExpiresAt() != null) {
                contract.setExpiresAt(request.getExpiresAt());
            }
            
            // Nếu có orderId, enrich terms như trong createContract
            if (request.getOrderId() != null) {
                RentalOrder linkedOrder = rentalOrderRepository.findById(request.getOrderId()).orElse(null);
                if (linkedOrder != null) {
                    List<OrderDetail> orderDetails = orderDetailRepository.findByRentalOrder_OrderId(request.getOrderId());
                    String enrichedTerms = appendDeviceTerms(contract.getTermsAndConditions(), linkedOrder, orderDetails);
                    contract.setTermsAndConditions(enrichedTerms);
                }
                contract.setOrderId(request.getOrderId());
            }
            
            // Cập nhật thông tin người chỉnh sửa
            contract.setUpdatedBy(updatedBy);
            contract.setUpdatedAt(LocalDateTime.now());
            
            // Lưu vào database
            return contractRepository.save(contract);
            
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Không thể cập nhật hợp đồng: " + e.getMessage());
        }
    }

    /**
     * Tạo hợp đồng tự động từ đơn thuê (RentalOrder)
     */
    @Override
    public Contract createContractFromOrder(Long orderId, Long createdBy) {
        try {
            // Lấy thông tin đơn thuê
            RentalOrder order = rentalOrderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn thuê với ID: " + orderId));
            
            // Kiểm tra xem đã có contract ACTIVE hoặc SIGNED cho order này chưa
            List<Contract> existingContracts = contractRepository.findByOrderId(orderId);
            boolean hasActiveContract = existingContracts.stream()
                    .anyMatch(c -> c.getStatus() == ContractStatus.ACTIVE || c.getStatus() == ContractStatus.SIGNED);
            
            if (hasActiveContract) {
                throw new RuntimeException(
                    "Đơn thuê này đã có hợp đồng đang có hiệu lực (ACTIVE/SIGNED). " +
                    "Không thể tạo hợp đồng mới. Vui lòng hủy hợp đồng cũ trước."
                );
            }
            
            // Tự động cancel các contract DRAFT cũ của order này
            List<Contract> draftContracts = existingContracts.stream()
                    .filter(c -> c.getStatus() == ContractStatus.DRAFT)
                    .collect(Collectors.toList());
            
            for (Contract draftContract : draftContracts) {
                try {
                    draftContract.setStatus(ContractStatus.CANCELLED);
                    draftContract.setUpdatedBy(createdBy);
                    draftContract.setUpdatedAt(LocalDateTime.now());
                    String currentDesc = draftContract.getDescription() != null ? draftContract.getDescription() : "";
                    draftContract.setDescription(currentDesc + "\n\n[Đã hủy tự động] Được thay thế bởi hợp đồng mới.");
                    contractRepository.save(draftContract);
                    log.info("Đã tự động hủy hợp đồng DRAFT cũ: {}", draftContract.getContractNumber());
                } catch (Exception e) {
                    log.warn("Không thể hủy hợp đồng DRAFT cũ {}: {}", draftContract.getContractId(), e.getMessage());
                }
            }
            
            // Lấy chi tiết đơn thuê
            List<OrderDetail> orderDetails = orderDetailRepository.findByRentalOrder_OrderId(orderId);
            
            // Tính số ngày thuê
            LocalDateTime rentalStart = order.getEffectiveStartDate();
            LocalDateTime rentalEnd = order.getEffectiveEndDate();
            long days = ChronoUnit.DAYS.between(rentalStart, rentalEnd);
            Integer rentalPeriodDays = (int) days;
            
            // Tạo mô tả từ các thiết bị trong đơn
            String description = "Hợp đồng thuê thiết bị từ đơn thuê #" + orderId + ". ";
            if (!orderDetails.isEmpty()) {
                String devices = orderDetails.stream()
                        .map(od -> od.getQuantity() + "x " + od.getDeviceModel().getDeviceName() + " (" + od.getDeviceModel().getBrand() + ")")
                        .collect(Collectors.joining(", "));
                description += "Thiết bị: " + devices;
            }
            
            // Tạo tiêu đề
            String title = "Hợp đồng thuê thiết bị - Đơn #" + orderId;
            
            // Tạo nội dung hợp đồng
            String contractContent = buildContractContent(order, orderDetails);
            
            // Tạo điều khoản
            String termsAndConditions = buildTermsAndConditions(order, orderDetails);
            termsAndConditions = appendDeviceTerms(termsAndConditions, order, orderDetails);
            
            // Tạo contract number
            String contractNumber = generateContractNumber();
            
            // Tạo hợp đồng
            Contract contract = Contract.builder()
                    .contractNumber(contractNumber)
                    .title(title)
                    .description(description)
                    .contractType(ContractType.EQUIPMENT_RENTAL)
                    .status(ContractStatus.DRAFT)
                    .customerId(order.getCustomer().getCustomerId())
                    .orderId(orderId) // Link to rental order
                    .contractContent(contractContent)
                    .termsAndConditions(termsAndConditions)
                    .rentalPeriodDays(rentalPeriodDays)
                    .totalAmount(order.getTotalPrice())
                    .depositAmount(order.getDepositAmount())
                    .startDate(rentalStart)
                    .endDate(rentalEnd)
                    .expiresAt(order.getEffectiveEndDate().plusDays(7)) // Hết hạn sau 7 ngày kể từ ngày kết thúc
                    .createdBy(createdBy)
                    .build();

            Contract savedContract = contractRepository.save(contract);
            return savedContract;
            
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo hợp đồng từ đơn thuê: " + e.getMessage());
        }
    }

    private void notifyCustomerContractReadyForSignature(Contract contract) {
        if (contract == null || contract.getCustomerId() == null) {
            return;
        }
        try {
            Customer customer = customerRepository.findById(contract.getCustomerId()).orElse(null);
            if (customer == null || customer.getAccount() == null) {
                return;
            }
            notificationService.notifyAccount(
                    customer.getAccount().getAccountId(),
                    NotificationType.CONTRACT_SIGNATURE_REQUIRED,
                    "Hợp đồng cần bạn ký",
                    "Hợp đồng " + contract.getContractNumber() + " đã được admin ký, vui lòng hoàn tất chữ ký."
            );
        } catch (Exception ex) {
            log.error("Không thể gửi thông báo ký hợp đồng cho customer {}: {}", contract.getCustomerId(), ex.getMessage());
        }
    }

    /**
     * Tạo nội dung hợp đồng
     */
    private String buildContractContent(RentalOrder order, List<OrderDetail> orderDetails) {
        StringBuilder content = new StringBuilder();
        content.append("<h2>HỢP ĐỒNG THUÊ THIẾT BỊ CÔNG NGHỆ</h2>");
        content.append("<p><strong>Đơn thuê:</strong> #").append(order.getOrderId()).append("</p>");
        content.append("<p><strong>Ngày bắt đầu:</strong> ").append(order.getEffectiveStartDate()).append("</p>");
        content.append("<p><strong>Ngày kết thúc:</strong> ").append(order.getEffectiveEndDate()).append("</p>");
        content.append("<p><strong>Số ngày thuê:</strong> ").append(ChronoUnit.DAYS.between(order.getEffectiveStartDate(), order.getEffectiveEndDate())).append(" ngày</p>");
        content.append("<h3>Thiết bị thuê:</h3>");
        content.append("<ul>");
        for (OrderDetail detail : orderDetails) {
            content.append("<li>")
                    .append(detail.getQuantity()).append("x ")
                    .append(detail.getDeviceModel().getDeviceName())
                    .append(" (").append(detail.getDeviceModel().getBrand()).append(")")
                    .append(" - Giá/ngày: ").append(detail.getPricePerDay())
                    .append(" - Tiền cọc: ").append(detail.getDepositAmountPerUnit().multiply(BigDecimal.valueOf(detail.getQuantity())))
                    .append("</li>");
        }
        content.append("</ul>");
        content.append("<p><strong>Tổng tiền thuê:</strong> ").append(order.getTotalPrice()).append(" VNĐ</p>");
        content.append("<p><strong>Tiền cọc:</strong> ").append(order.getDepositAmount()).append(" VNĐ</p>");
        return content.toString();
    }
    
    /**
     * Tạo điều khoản và điều kiện
     */
    private String buildTermsAndConditions(RentalOrder order, List<OrderDetail> orderDetails) {
        StringBuilder terms = new StringBuilder();
        terms.append("ĐIỀU KHOẢN VÀ ĐIỀU KIỆN THUÊ THIẾT BỊ\n\n");
        terms.append("1. Khách hàng có trách nhiệm bảo quản thiết bị trong thời gian thuê.\n");
        terms.append("2. Nếu thiết bị bị hư hỏng hoặc mất mát, khách hàng sẽ phải chịu chi phí sửa chữa hoặc thay thế.\n");
        terms.append("3. Tiền cọc sẽ được hoàn trả sau khi thiết bị được kiểm tra và không có hư hỏng.\n");
        terms.append("4. Khách hàng phải trả lại thiết bị đúng hạn, nếu quá hạn sẽ bị phạt 10% giá trị thiết bị mỗi ngày.\n");
        terms.append("5. Mọi tranh chấp sẽ được giải quyết theo pháp luật Việt Nam.\n");
        return terms.toString();
    }

    private String appendDeviceTerms(String baseTerms, RentalOrder order, List<OrderDetail> orderDetails) {
        List<DeviceContractTerm> deviceTerms = deviceContractTermService.findApplicableTerms(order, orderDetails);
        if (deviceTerms.isEmpty()) {
            return baseTerms;
        }
        StringBuilder builder = new StringBuilder(baseTerms != null ? baseTerms.trim() : "");
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append("ĐIỀU KHOẢN RIÊNG CHO THIẾT BỊ\n");
        int index = 1;
        for (DeviceContractTerm term : deviceTerms) {
            builder.append(index++)
                    .append(". [")
                    .append(resolveScopeLabel(term))
                    .append("] ")
                    .append(term.getTitle())
                    .append("\n")
                    .append(term.getContent())
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private String resolveScopeLabel(DeviceContractTerm term) {
        if (term.getDeviceModel() != null) {
            return "Model " + term.getDeviceModel().getDeviceName();
        }
        if (term.getDeviceCategory() != null) {
            return "Loại " + term.getDeviceCategory().getDeviceCategoryName();
        }
        return "Mặc định";
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
            
            // Chuyển sang trạng thái chờ admin ký trước
            contract.setStatus(ContractStatus.PENDING_ADMIN_SIGNATURE);
            contract.setUpdatedBy(sentBy);
            
            return contractRepository.save(contract);
            
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi hợp đồng để ký: " + e.getMessage());
        }
    }

    @Override
    public Contract cancelContract(Long contractId, String reason, Long cancelledBy) {
        try {
            Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId));
            
            // Không cho phép cancel contract đã ACTIVE hoặc SIGNED
            if (contract.getStatus() == ContractStatus.ACTIVE || contract.getStatus() == ContractStatus.SIGNED) {
                throw new RuntimeException(
                    "Không thể hủy hợp đồng ở trạng thái " + contract.getStatus().getDisplayName() + 
                    ". Chỉ có thể hủy hợp đồng chưa có hiệu lực."
                );
            }
            
            // Set status thành CANCELLED
            contract.setStatus(ContractStatus.CANCELLED);
            contract.setUpdatedBy(cancelledBy);
            contract.setUpdatedAt(LocalDateTime.now());
            
            // Lưu lý do hủy vào description nếu có
            if (reason != null && !reason.trim().isEmpty()) {
                String currentDesc = contract.getDescription() != null ? contract.getDescription() : "";
                contract.setDescription(currentDesc + "\n\n[Đã hủy] Lý do: " + reason);
            }
            
            return contractRepository.save(contract);
            
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Không thể hủy hợp đồng: " + e.getMessage());
        }
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
            
            // Kiểm tra trạng thái hợp đồng (chờ customer ký)
            if (contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
                return false;
            }
            
            // Kiểm tra admin đã ký chưa
            if (contract.getAdminSignedAt() == null || contract.getAdminSignedBy() == null) {
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
            
            // Kiểm tra signature method hợp lệ (optional)
            if (request.getSignatureMethod() != null && !request.getSignatureMethod().trim().isEmpty()) {
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
            System.err.println("Lỗi kiểm tra dữ liệu chữ ký: " + e.getMessage());
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
