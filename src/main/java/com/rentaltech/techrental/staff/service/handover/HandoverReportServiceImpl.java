package com.rentaltech.techrental.staff.service.handover;

import com.rentaltech.techrental.contract.service.EmailService;
import com.rentaltech.techrental.contract.service.SMSService;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.OrderDetailRepository;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.staff.model.HandoverReport;
import com.rentaltech.techrental.staff.model.HandoverReportItem;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.dto.HandoverPinDeliveryDto;
import com.rentaltech.techrental.staff.model.dto.HandoverReportCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.HandoverReportResponseDto;
import com.rentaltech.techrental.staff.repository.HandoverReportRepository;
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
    private final SMSService smsService;
    private final EmailService emailService;

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
    public HandoverReportResponseDto createReport(HandoverReportCreateRequestDto request, List<MultipartFile> evidences) {
        Objects.requireNonNull(request, "request must not be null");

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + request.getTaskId()));

        RentalOrder rentalOrder = rentalOrderRepository.findById(task.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Rental order not found: " + task.getOrderId()));

        validatePinForOrder(rentalOrder.getOrderId(), request.getPinCode());

        List<HandoverReportItem> items = resolveItems(request, rentalOrder);
        List<String> evidenceUrls = uploadEvidence(task.getTaskId(), evidences);

        HandoverReport report = HandoverReport.builder()
                .task(task)
                .rentalOrder(rentalOrder)
                .customerInfo(request.getCustomerInfo())
                .technicianInfo(request.getTechnicianInfo())
                .handoverDateTime(request.getHandoverDateTime() != null
                        ? request.getHandoverDateTime()
                        : LocalDateTime.now())
                .handoverLocation(request.getHandoverLocation())
                .customerSignature(request.getCustomerSignature())
                .items(items)
                .evidenceUrls(evidenceUrls)
                .build();

        HandoverReport saved = handoverReportRepository.save(report);
        
        // Update rental order status to IN_USE after successful handover
        rentalOrder.setOrderStatus(OrderStatus.IN_USE);
        rentalOrderRepository.save(rentalOrder);
        
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

    private void validatePinForOrder(Long orderId, String providedPin) {
        if (!StringUtils.hasText(providedPin)) {
            throw new IllegalArgumentException("PIN code is required");
        }
        String key = buildPinKeyForOrder(orderId);
        String storedPin = getPinCode(key);
        if (storedPin == null) {
            throw new IllegalArgumentException("PIN has expired or not requested for order " + orderId);
        }
        if (!storedPin.equals(providedPin)) {
            throw new IllegalArgumentException("Invalid PIN code for order " + orderId);
        }
        deletePinCode(key);
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