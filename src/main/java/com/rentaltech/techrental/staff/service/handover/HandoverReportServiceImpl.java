package com.rentaltech.techrental.staff.service.handover;

import com.rentaltech.techrental.contract.service.EmailService;
import com.rentaltech.techrental.contract.service.SMSService;
import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.OrderDetailRepository;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.staff.model.DeviceQualityInfo;
import com.rentaltech.techrental.staff.model.HandoverReport;
import com.rentaltech.techrental.staff.model.HandoverReportItem;
import com.rentaltech.techrental.staff.model.HandoverReportStatus;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.model.dto.*;
import com.rentaltech.techrental.staff.repository.HandoverReportRepository;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HandoverReportServiceImpl implements HandoverReportService {

    private final TaskRepository taskRepository;
    private final RentalOrderRepository rentalOrderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final HandoverReportRepository handoverReportRepository;
    private final ImageStorageService imageStorageService;
    private final AllocationRepository allocationRepository;
    private final DeviceRepository deviceRepository;
    private final SMSService smsService;
    private final EmailService emailService;
    private final AccountService accountService;
    private final StaffRepository staffRepository;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private final ConcurrentHashMap<String, PinCacheEntry> pinCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    private static final long PIN_TTL_SECONDS = 300;

    @PostConstruct
    void init() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredPins, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    void destroy() {
        cleanupExecutor.shutdown();
    }

    @Override
    @Transactional
    public HandoverReportResponseDto createReport(HandoverReportCreateRequestDto request, List<MultipartFile> evidences, String staffUsername) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(staffUsername, "staffUsername must not be null");

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + request.getTaskId()));

        RentalOrder rentalOrder = rentalOrderRepository.findById(task.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Rental order not found: " + task.getOrderId()));

        Account staffAccount = accountService.getByUsername(staffUsername)
                .orElseThrow(() -> new IllegalArgumentException("Staff account not found: " + staffUsername));

        Staff createdByStaff = staffRepository.findByAccount_AccountId(staffAccount.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Staff not found for account: " + staffUsername));

        List<HandoverReportItem> items = resolveItems(request, rentalOrder);
        List<String> evidenceUrls = uploadEvidence(task.getTaskId(), evidences);
        List<DeviceQualityInfo> deviceQualityInfos = resolveDeviceQualityInfos(request, rentalOrder);

        HandoverReport report = HandoverReport.builder()
                .task(task)
                .rentalOrder(rentalOrder)
                .createdByStaff(createdByStaff)
                .customerInfo(request.getCustomerInfo())
                .technicianInfo(request.getTechnicianInfo())
                .handoverDateTime(request.getHandoverDateTime() != null
                        ? request.getHandoverDateTime()
                        : LocalDateTime.now())
                .handoverLocation(request.getHandoverLocation())
                .customerSignature(request.getCustomerSignature())
                .status(HandoverReportStatus.PENDING_STAFF_SIGNATURE)
                .staffSigned(false)
                .customerSigned(false)
                .items(items)
                .evidenceUrls(evidenceUrls)
                .deviceQualityInfos(deviceQualityInfos)
                .build();

        HandoverReport saved = handoverReportRepository.save(report);

        // Send PIN to staff email
        String pinCode = generatePinCode();
        String staffEmail = staffAccount.getEmail();
        if (!StringUtils.hasText(staffEmail)) {
            throw new IllegalStateException("Staff account does not have email to receive PIN");
        }
        boolean emailSent = emailService.sendOTP(staffEmail, pinCode);
        if (!emailSent) {
            throw new IllegalStateException("Unable to send PIN to staff email: " + staffEmail);
        }
        savePinCode(buildPinKeyForReport(saved.getHandoverReportId()), pinCode);

        if (task.getStatus() != TaskStatus.COMPLETED) {
            task.setStatus(TaskStatus.COMPLETED);
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            }
            taskRepository.save(task);
        }

        // Don't change order status yet - wait for both signatures

        return HandoverReportResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public HandoverPinDeliveryDto sendPinForOrder(Long orderId) {
        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Rental order not found: " + orderId));

        Customer customer = order.getCustomer();
        if (customer == null) {
            throw new IllegalArgumentException("Rental order " + orderId + " is not linked to any customer");
        }

        String phone = StringUtils.hasText(customer.getPhoneNumber())
                ? customer.getPhoneNumber()
                : (customer.getAccount() != null ? customer.getAccount().getPhoneNumber() : null);
        String email = StringUtils.hasText(customer.getEmail())
                ? customer.getEmail()
                : (customer.getAccount() != null ? customer.getAccount().getEmail() : null);

        if (!StringUtils.hasText(phone) && !StringUtils.hasText(email)) {
            throw new IllegalStateException("Customer " + customer.getCustomerId() + " does not have phone or email to receive PIN");
        }

        String pinCode = generatePinCode();
        boolean smsSent = false;
        boolean emailSent = false;

        if (StringUtils.hasText(phone)) {
            smsSent = smsService.sendOTP(phone, pinCode);
        }
        if (StringUtils.hasText(email)) {
            emailSent = emailService.sendOTP(email, pinCode);
        }

        if (!smsSent && !emailSent) {
            throw new IllegalStateException("Unable to deliver PIN for order " + orderId);
        }

        savePinCode(buildPinKeyForOrder(orderId), pinCode);

        String customerName = StringUtils.hasText(customer.getFullName())
                ? customer.getFullName()
                : (customer.getAccount() != null ? customer.getAccount().getUsername() : null);

        return HandoverPinDeliveryDto.builder()
                .orderId(orderId)
                .customerId(customer.getCustomerId())
                .customerName(customerName)
                .phoneNumber(phone)
                .email(email)
                .smsSent(smsSent)
                .emailSent(emailSent)
                .build();
    }

    @Override
    @Transactional
    public HandoverReportResponseDto getReport(Long handoverReportId) {
        return handoverReportRepository.findById(handoverReportId)
                .map(HandoverReportResponseDto::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Handover report not found: " + handoverReportId));
    }

    @Override
    @Transactional
    public List<HandoverReportResponseDto> getReportsByTask(Long taskId) {
        return handoverReportRepository.findByTask_TaskId(taskId).stream()
                .map(HandoverReportResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<HandoverReportResponseDto> getReportsByOrder(Long orderId) {
        return handoverReportRepository.findByRentalOrder_OrderId(orderId).stream()
                .map(HandoverReportResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<HandoverReportResponseDto> getReportsByTechnician(Long staffId) {
        return handoverReportRepository.findByTechnician(staffId).stream()
                .map(HandoverReportResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<HandoverReportResponseDto> getAllReports() {
        return handoverReportRepository.findAll().stream()
                .map(HandoverReportResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HandoverPinDeliveryDto sendPinToStaffForReport(Long handoverReportId) {
        HandoverReport report = handoverReportRepository.findById(handoverReportId)
                .orElseThrow(() -> new IllegalArgumentException("Handover report not found: " + handoverReportId));

        if (report.getStatus() != HandoverReportStatus.PENDING_STAFF_SIGNATURE) {
            throw new IllegalStateException("Report is not in PENDING_STAFF_SIGNATURE status");
        }

        Task task = report.getTask();
        if (task == null || task.getAssignedStaff() == null || task.getAssignedStaff().isEmpty()) {
            throw new IllegalStateException("No assigned staff found for this report");
        }

        Staff staff = task.getAssignedStaff().iterator().next();
        Account staffAccount = staff.getAccount();
        if (staffAccount == null) {
            throw new IllegalStateException("Staff account not found");
        }

        String email = staffAccount.getEmail();
        if (!StringUtils.hasText(email)) {
            throw new IllegalStateException("Staff account does not have email to receive PIN");
        }

        String pinCode = generatePinCode();
        boolean emailSent = emailService.sendOTP(email, pinCode);
        if (!emailSent) {
            throw new IllegalStateException("Unable to send PIN to staff email: " + email);
        }

        savePinCode(buildPinKeyForReport(handoverReportId), pinCode);

        return HandoverPinDeliveryDto.builder()
                .orderId(report.getRentalOrder().getOrderId())
                .email(email)
                .emailSent(true)
                .smsSent(false)
                .build();
    }

    @Override
    @Transactional
    public HandoverReportResponseDto signByStaff(Long handoverReportId, HandoverReportStaffSignRequestDto request) {
        HandoverReport report = handoverReportRepository.findById(handoverReportId)
                .orElseThrow(() -> new IllegalArgumentException("Handover report not found: " + handoverReportId));

        if (report.getStaffSigned()) {
            throw new IllegalStateException("Report has already been signed by staff");
        }

        String key = buildPinKeyForReport(handoverReportId);
        String storedPin = getPinCode(key);
        if (storedPin == null) {
            throw new IllegalArgumentException("PIN has expired or not requested for report " + handoverReportId);
        }
        if (!storedPin.equals(request.getPinCode())) {
            throw new IllegalArgumentException("Invalid PIN code for report " + handoverReportId);
        }

        report.setStaffSigned(true);
        report.setStaffSignedAt(LocalDateTime.now());
        if (StringUtils.hasText(request.getStaffSignature())) {
            report.setStaffSignature(request.getStaffSignature());
        }
        report.setStatus(HandoverReportStatus.STAFF_SIGNED);

        HandoverReport saved = handoverReportRepository.save(report);
        deletePinCode(key);

        // Check if both signed, then update order status
        checkAndUpdateOrderStatusIfBothSigned(saved);

        return HandoverReportResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public HandoverPinDeliveryDto sendPinToCustomerForReport(Long handoverReportId, String email) {
        HandoverReport report = handoverReportRepository.findById(handoverReportId)
                .orElseThrow(() -> new IllegalArgumentException("Handover report not found: " + handoverReportId));

        if (report.getStatus() != HandoverReportStatus.STAFF_SIGNED) {
            throw new IllegalStateException("Report must be signed by staff before customer can sign");
        }

        if (report.getCustomerSigned()) {
            throw new IllegalStateException("Report has already been signed by customer");
        }

        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required");
        }

        String pinCode = generatePinCode();
        boolean emailSent = emailService.sendOTP(email, pinCode);
        if (!emailSent) {
            throw new IllegalStateException("Unable to send PIN to email: " + email);
        }

        savePinCode(buildPinKeyForReport(handoverReportId), pinCode);

        return HandoverPinDeliveryDto.builder()
                .orderId(report.getRentalOrder().getOrderId())
                .email(email)
                .emailSent(true)
                .smsSent(false)
                .build();
    }

    @Override
    @Transactional
    public HandoverReportResponseDto signByCustomer(Long handoverReportId, HandoverReportCustomerSignRequestDto request) {
        HandoverReport report = handoverReportRepository.findById(handoverReportId)
                .orElseThrow(() -> new IllegalArgumentException("Handover report not found: " + handoverReportId));

        if (report.getStatus() != HandoverReportStatus.STAFF_SIGNED) {
            throw new IllegalStateException("Report must be signed by staff before customer can sign");
        }

        if (report.getCustomerSigned()) {
            throw new IllegalStateException("Report has already been signed by customer");
        }

        String key = buildPinKeyForReport(handoverReportId);
        String storedPin = getPinCode(key);
        if (storedPin == null) {
            throw new IllegalArgumentException("PIN has expired or not requested for report " + handoverReportId);
        }
        if (!storedPin.equals(request.getPinCode())) {
            throw new IllegalArgumentException("Invalid PIN code for report " + handoverReportId);
        }

        report.setCustomerSigned(true);
        report.setCustomerSignedAt(LocalDateTime.now());
        if (StringUtils.hasText(request.getCustomerSignature())) {
            report.setCustomerSignature(request.getCustomerSignature());
        }
        report.setStatus(HandoverReportStatus.BOTH_SIGNED);

        HandoverReport saved = handoverReportRepository.save(report);
        deletePinCode(key);

        // Update order status to IN_USE when both signed
        checkAndUpdateOrderStatusIfBothSigned(saved);

        return HandoverReportResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public List<HandoverReportResponseDto> getReportsByCustomerOrder(Long customerId) {
        return handoverReportRepository.findByRentalOrder_Customer_CustomerId(customerId).stream()
                .map(HandoverReportResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    private void checkAndUpdateOrderStatusIfBothSigned(HandoverReport report) {
        if (report.getStatus() == HandoverReportStatus.BOTH_SIGNED && report.getStaffSigned() && report.getCustomerSigned()) {
            RentalOrder order = report.getRentalOrder();
            if (order != null && order.getOrderStatus() != OrderStatus.IN_USE) {
                order.setOrderStatus(OrderStatus.IN_USE);
                rentalOrderRepository.save(order);
                markDevicesAsRenting(order.getOrderId());
                incrementDeviceUsageCount(order.getOrderId());
            }
        }
    }

    private void incrementDeviceUsageCount(Long orderId) {
        List<Device> devices = allocationRepository.findByOrderDetail_RentalOrder_OrderId(orderId).stream()
                .map(Allocation::getDevice)
                .filter(Objects::nonNull)
                .peek(device -> {
                    Integer currentCount = device.getUsageCount() != null ? device.getUsageCount() : 0;
                    device.setUsageCount(currentCount + 1);
                })
                .collect(Collectors.toList());
        if (!devices.isEmpty()) {
            deviceRepository.saveAll(devices);
        }
    }

    private List<HandoverReportItem> resolveItems(HandoverReportCreateRequestDto request, RentalOrder rentalOrder) {
        if (!CollectionUtils.isEmpty(request.getItems())) {
            return request.getItems().stream()
                    .filter(Objects::nonNull)
                    .map(itemDto -> {
                        HandoverReportItem entity = itemDto.toEntity();
                        if (!StringUtils.hasText(entity.getUnit())) {
                            entity.setUnit("unit");
                        }
                        if (entity.getDeliveredQuantity() == null) {
                            entity.setDeliveredQuantity(entity.getOrderedQuantity());
                        }
                        return entity;
                    })
                    .collect(Collectors.toList());
        }

        List<OrderDetail> orderDetails = orderDetailRepository.findByRentalOrder_OrderId(rentalOrder.getOrderId());
        if (CollectionUtils.isEmpty(orderDetails)) {
            log.warn("No order details found for order {}", rentalOrder.getOrderId());
            return List.of();
        }
        return orderDetails.stream()
                .map(detail -> {
                    HandoverReportItem item = HandoverReportItem.fromOrderDetail(detail);
                    if (!StringUtils.hasText(item.getUnit())) {
                        item.setUnit(resolveUnit(detail));
                    }
                    return item;
                })
                .collect(Collectors.toList());
    }

    private String resolveUnit(OrderDetail detail) {
        DeviceModel deviceModel = detail.getDeviceModel();
        if (deviceModel != null && deviceModel.getDeviceCategory() != null) {
            return deviceModel.getDeviceCategory().getDeviceCategoryName();
        }
        return "unit";
    }

    private List<DeviceQualityInfo> resolveDeviceQualityInfos(HandoverReportCreateRequestDto request, RentalOrder rentalOrder) {
        // If device quality infos are provided in request, use them
        if (!CollectionUtils.isEmpty(request.getDeviceQualityInfos())) {
            return request.getDeviceQualityInfos().stream()
                    .filter(Objects::nonNull)
                    .map(DeviceQualityInfoDto::toEntity)
                    .collect(Collectors.toList());
        }

        // Otherwise, auto-populate from allocations
        List<Allocation> allocations = allocationRepository.findByOrderDetail_RentalOrder_OrderId(rentalOrder.getOrderId());
        if (CollectionUtils.isEmpty(allocations)) {
            log.warn("No allocations found for order {}", rentalOrder.getOrderId());
            return List.of();
        }

        return allocations.stream()
                .filter(allocation -> allocation.getDevice() != null)
                .map(allocation -> {
                    Device device = allocation.getDevice();
                    DeviceModel model = device.getDeviceModel();
                    String modelName = model != null ? model.getDeviceName() : null;
                    
                    return DeviceQualityInfo.builder()
                            .deviceSerialNumber(device.getSerialNumber())
                            .qualityStatus("GOOD") // Default status, can be updated later
                            .qualityDescription(null) // No description by default
                            .deviceModelName(modelName)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<String> uploadEvidence(Long taskId, List<MultipartFile> evidences) {
        if (CollectionUtils.isEmpty(evidences)) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        int index = 1;
        for (MultipartFile file : evidences) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                String label = "handover-" + index++;
                String url = imageStorageService.uploadHandoverEvidence(file, taskId, label);
                urls.add(url);
            } catch (Exception ex) {
                log.warn("Failed to upload evidence for task {}: {}", taskId, ex.getMessage());
            }
        }
        return urls;
    }

    private String generatePinCode() {
        Random random = new Random();
        int pin = 100000 + random.nextInt(900000);
        return String.valueOf(pin);
    }

    private void savePinCode(String key, String pinCode) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, pinCode, PIN_TTL_SECONDS, TimeUnit.SECONDS);
                return;
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable for handover pin storage, fallback to in-memory cache: {}", ex.getMessage());
        }
        long expiry = System.currentTimeMillis() + PIN_TTL_SECONDS * 1000;
        pinCache.put(key, new PinCacheEntry(pinCode, expiry));
    }

    private String getPinCode(String key) {
        try {
            if (redisTemplate != null) {
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    return value.toString();
                }
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable for handover pin retrieval: {}", ex.getMessage());
        }

        PinCacheEntry entry = pinCache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            pinCache.remove(key);
            return null;
        }
        return entry.pin;
    }

    private void deletePinCode(String key) {
        try {
            if (redisTemplate != null) {
                redisTemplate.delete(key);
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable for handover pin deletion: {}", ex.getMessage());
        }
        pinCache.remove(key);
    }

    private void cleanupExpiredPins() {
        pinCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private String buildPinKeyForOrder(Long orderId) {
        return "handover_pin_order_" + orderId;
    }

    private String buildPinKeyForReport(Long handoverReportId) {
        return "handover_pin_report_" + handoverReportId;
    }

    private void markDevicesAsRenting(Long orderId) {
        List<Device> devices = allocationRepository.findByOrderDetail_RentalOrder_OrderId(orderId).stream()
                .map(Allocation::getDevice)
                .filter(Objects::nonNull)
                .peek(device -> device.setStatus(DeviceStatus.RENTING))
                .collect(Collectors.toList());
        if (devices.isEmpty()) {
            log.warn("No allocated devices found to mark as RENTING for order {}", orderId);
            return;
        }
        deviceRepository.saveAll(devices);
    }

    private static class PinCacheEntry {
        final String pin;
        final long expiryTime;

        PinCacheEntry(String pin, long expiryTime) {
            this.pin = pin;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
