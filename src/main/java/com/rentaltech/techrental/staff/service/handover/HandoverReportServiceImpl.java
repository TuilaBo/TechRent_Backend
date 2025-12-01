package com.rentaltech.techrental.staff.service.handover;

import com.rentaltech.techrental.contract.service.EmailService;
import com.rentaltech.techrental.contract.service.SMSService;
import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.AllocationConditionDetail;
import com.rentaltech.techrental.device.model.AllocationConditionSnapshot;
import com.rentaltech.techrental.device.model.AllocationSnapshotSource;
import com.rentaltech.techrental.device.model.AllocationSnapshotType;
import com.rentaltech.techrental.device.model.ConditionDefinition;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.model.DiscrepancyCreatedFrom;
import com.rentaltech.techrental.device.model.DiscrepancyReport;
import com.rentaltech.techrental.device.model.dto.DiscrepancyInlineRequestDto;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportRequestDto;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import com.rentaltech.techrental.device.repository.AllocationConditionSnapshotRepository;
import com.rentaltech.techrental.device.repository.ConditionDefinitionRepository;
import com.rentaltech.techrental.device.repository.DiscrepancyReportRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.device.service.AllocationSnapshotService;
import com.rentaltech.techrental.device.service.DiscrepancyReportService;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.OrderDetailRepository;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.staff.model.HandoverReport;
import com.rentaltech.techrental.staff.model.HandoverReportItem;
import com.rentaltech.techrental.staff.model.HandoverReportStatus;
import com.rentaltech.techrental.staff.model.HandoverType;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.model.dto.*;
import com.rentaltech.techrental.staff.repository.HandoverReportRepository;
import com.rentaltech.techrental.staff.repository.HandoverReportItemRepository;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.staff.service.settlementservice.SettlementService;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HandoverReportServiceImpl implements HandoverReportService {

    private final TaskRepository taskRepository;
    private final RentalOrderRepository rentalOrderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final HandoverReportRepository handoverReportRepository;
    private final AllocationConditionSnapshotRepository allocationConditionSnapshotRepository;
    private final ImageStorageService imageStorageService;
    private final AllocationRepository allocationRepository;
    private final HandoverReportItemRepository handoverReportItemRepository;
    private final DeviceRepository deviceRepository;
    private final ConditionDefinitionRepository conditionDefinitionRepository;
    private final SMSService smsService;
    private final EmailService emailService;
    private final AccountService accountService;
    private final StaffRepository staffRepository;
    private final DiscrepancyReportService discrepancyReportService;
    private final DiscrepancyReportRepository discrepancyReportRepository;
    private final AllocationSnapshotService allocationSnapshotService;
    private final SettlementService settlementService;

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
    public HandoverReportResponseDto createCheckoutReport(HandoverReportCreateOutRequestDto request, String staffUsername) {
        return createReportInternal(request, HandoverType.CHECKOUT, List.of(), request.getDeviceConditions(), staffUsername);
    }

    @Override
    @Transactional
    public HandoverReportResponseDto createCheckinReport(HandoverReportCreateInRequestDto request, String staffUsername) {
        return createReportInternal(request, HandoverType.CHECKIN, request.getDiscrepancies(), List.of(), staffUsername);
    }

    @Override
    @Transactional
    public HandoverReportResponseDto updateCheckoutReport(Long reportId, HandoverReportUpdateOutRequestDto request, String staffUsername) {
        return updateReportInternal(reportId, request, HandoverType.CHECKOUT, List.of(), request.getDeviceConditions(), staffUsername);
    }

    @Override
    @Transactional
    public HandoverReportResponseDto updateCheckinReport(Long reportId, HandoverReportUpdateInRequestDto request, String staffUsername) {
        return updateReportInternal(reportId, request, HandoverType.CHECKIN, request.getDiscrepancies(), List.of(), staffUsername);
    }

    private HandoverReportResponseDto createReportInternal(HandoverReportBaseCreateRequestDto request,
                                                           HandoverType handoverType,
                                                           List<DiscrepancyInlineRequestDto> discrepancies,
                                                           List<HandoverDeviceConditionRequestDto> deviceConditions,
                                                           String staffUsername) {
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
                .handoverType(handoverType)
                .status(HandoverReportStatus.PENDING_STAFF_SIGNATURE)
                .staffSigned(false)
                .customerSigned(false)
                .items(items)
                .build();

        report.getItems().forEach(item -> item.setHandoverReport(report));

        HandoverReport saved = handoverReportRepository.save(report);
        if (handoverType == HandoverType.CHECKIN) {
            createHandoverSnapshots(saved.getItems(), handoverType, createdByStaff);
            createDiscrepancies(discrepancies, saved.getHandoverReportId());
            markDevicesForPostRentalQc(rentalOrder, saved.getItems());
        } else {
            createSnapshotsFromDeviceConditions(saved, deviceConditions, createdByStaff);
        }

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

        return HandoverReportResponseDto.fromEntity(saved, findDiscrepancies(saved));
    }

    private HandoverReportResponseDto updateReportInternal(Long reportId,
                                                           HandoverReportBaseUpdateRequestDto request,
                                                           HandoverType expectedType,
                                                           List<DiscrepancyInlineRequestDto> discrepancies,
                                                           List<HandoverDeviceConditionRequestDto> deviceConditions,
                                                           String staffUsername) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(staffUsername, "staffUsername must not be null");

        HandoverReport report = handoverReportRepository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy handover report: " + reportId));
        if (expectedType != null && report.getHandoverType() != expectedType) {
            throw new IllegalArgumentException("Biên bản không thuộc loại " + expectedType);
        }

        Account staffAccount = accountService.getByUsername(staffUsername)
                .orElseThrow(() -> new IllegalArgumentException("Staff account not found: " + staffUsername));
        Staff staff = staffRepository.findByAccount_AccountId(staffAccount.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Staff not found for account: " + staffUsername));

        report.setCustomerInfo(request.getCustomerInfo());
        report.setTechnicianInfo(request.getTechnicianInfo());
        report.setHandoverDateTime(request.getHandoverDateTime());
        report.setHandoverLocation(request.getHandoverLocation());
        report.setCustomerSignature(request.getCustomerSignature());

        RentalOrder rentalOrder = report.getRentalOrder();
        if (rentalOrder == null) {
            throw new IllegalStateException("Không xác định được đơn thuê của biên bản");
        }

        List<HandoverReportItem> updatedItems = resolveItems(request.getItems(), rentalOrder, true);
        report.getItems().clear();
        updatedItems.forEach(item -> item.setHandoverReport(report));
        report.getItems().addAll(updatedItems);

        HandoverReport saved = handoverReportRepository.save(report);
        handoverReportRepository.flush();

        Long orderId = rentalOrder.getOrderId();
        clearSnapshotsForOrder(orderId, expectedType);
        if (expectedType == HandoverType.CHECKIN) {
            createHandoverSnapshots(saved.getItems(), expectedType, staff);
            replaceDiscrepancies(saved.getHandoverReportId(), discrepancies);
            markDevicesForPostRentalQc(rentalOrder, saved.getItems());
        } else {
            createSnapshotsFromDeviceConditions(saved, deviceConditions, staff);
        }

        return HandoverReportResponseDto.fromEntity(saved, findDiscrepancies(saved));
    }

    @Override
    @Transactional
    public HandoverReportItemResponseDto addEvidence(Long itemId, MultipartFile file, String staffUsername) {
        Objects.requireNonNull(file, "file must not be null");
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }
        accountService.getByUsername(staffUsername)
                .orElseThrow(() -> new IllegalArgumentException("Staff account not found: " + staffUsername));
        HandoverReportItem item = handoverReportItemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy handover item: " + itemId));
        HandoverReport report = item.getHandoverReport();
        Long taskId = report != null && report.getTask() != null ? report.getTask().getTaskId() : null;
        if (taskId == null) {
            throw new IllegalStateException("Item không gắn với task nên không thể tải hình");
        }
        String label = "handover-item-extra-" + (item.getEvidenceUrls() == null ? 1 : item.getEvidenceUrls().size() + 1);
        String url = imageStorageService.uploadHandoverEvidence(file, taskId, label);
        if (item.getEvidenceUrls() == null) {
            item.setEvidenceUrls(new ArrayList<>());
        }
        item.getEvidenceUrls().add(url);
        HandoverReportItem saved = handoverReportItemRepository.save(item);
        return HandoverReportItemResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public HandoverReportItemResponseDto updateEvidenceByDevice(Long handoverReportId,
                                                                Long deviceId,
                                                                List<MultipartFile> files,
                                                                String staffUsername) {
        if (handoverReportId == null || deviceId == null) {
            throw new IllegalArgumentException("handoverReportId và deviceId không được để trống");
        }
        if (CollectionUtils.isEmpty(files)) {
            throw new IllegalArgumentException("Danh sách file không được để trống");
        }
        accountService.getByUsername(staffUsername)
                .orElseThrow(() -> new IllegalArgumentException("Staff account not found: " + staffUsername));
        HandoverReport report = handoverReportRepository.findById(handoverReportId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy biên bản bàn giao: " + handoverReportId));
        HandoverReportItem item = handoverReportItemRepository
                .findFirstByHandoverReport_HandoverReportIdAndAllocation_Device_DeviceId(handoverReportId, deviceId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Không tìm thấy thiết bị " + deviceId + " trong biên bản " + handoverReportId));
        Long taskId = report.getTask() != null ? report.getTask().getTaskId() : null;
        List<String> uploadedUrls = new ArrayList<>();
        int sequence = 1;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String label = String.format("device-%d-evidence-%d", deviceId, sequence++);
            String url = imageStorageService.uploadHandoverEvidence(file, taskId, label);
            uploadedUrls.add(url);
        }
        if (uploadedUrls.isEmpty()) {
            throw new IllegalArgumentException("Không có file hợp lệ để tải lên");
        }
        item.setEvidenceUrls(uploadedUrls);
        HandoverReportItem savedItem = handoverReportItemRepository.save(item);
        return HandoverReportItemResponseDto.fromEntity(savedItem);
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
                .map(report -> HandoverReportResponseDto.fromEntity(report, findDiscrepancies(report)))
                .orElseThrow(() -> new IllegalArgumentException("Handover report not found: " + handoverReportId));
    }

    @Override
    @Transactional
    public List<HandoverReportResponseDto> getReportsByTask(Long taskId) {
        return handoverReportRepository.findByTask_TaskId(taskId).stream()
                .map(report -> HandoverReportResponseDto.fromEntity(report, findDiscrepancies(report)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<HandoverReportResponseDto> getReportsByOrder(Long orderId) {
        return handoverReportRepository.findByRentalOrder_OrderId(orderId).stream()
                .map(report -> HandoverReportResponseDto.fromEntity(report, findDiscrepancies(report)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<HandoverReportResponseDto> getReportsByTechnician(Long staffId) {
        return handoverReportRepository.findByTechnician(staffId).stream()
                .map(report -> HandoverReportResponseDto.fromEntity(report, findDiscrepancies(report)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<HandoverReportResponseDto> getAllReports() {
        return handoverReportRepository.findAll().stream()
                .map(report -> HandoverReportResponseDto.fromEntity(report, findDiscrepancies(report)))
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

        return HandoverReportResponseDto.fromEntity(saved, findDiscrepancies(saved));
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
//        deletePinCode(key);

        // Update order status to IN_USE when both signed
        checkAndUpdateOrderStatusIfBothSigned(saved);
        createSettlementAfterCustomerSigned(saved);

        return HandoverReportResponseDto.fromEntity(saved, findDiscrepancies(saved));
    }

    @Override
    @Transactional
    public List<HandoverReportResponseDto> getReportsByCustomerOrder(Long customerId) {
        return handoverReportRepository.findByRentalOrder_Customer_CustomerId(customerId).stream()
                .map(report -> HandoverReportResponseDto.fromEntity(report, findDiscrepancies(report)))
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

    private void createSettlementAfterCustomerSigned(HandoverReport report) {
        if (report == null
                || report.getHandoverType() != HandoverType.CHECKIN
                || report.getRentalOrder() == null
                || report.getRentalOrder().getOrderId() == null) {
            return;
        }
        Long orderId = report.getRentalOrder().getOrderId();
        try {
            settlementService.createAutomaticForOrder(orderId);
        } catch (Exception ex) {
            log.warn("Không thể tạo settlement cho order {} sau khi khách ký handover: {}", orderId, ex.getMessage());
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

    private List<DiscrepancyReport> findDiscrepancies(HandoverReport report) {
        Long handoverReportId = Optional.ofNullable(report)
                .map(HandoverReport::getHandoverReportId)
                .orElse(null);
        if (handoverReportId == null) {
            return List.of();
        }
        return discrepancyReportRepository.findByCreatedFromAndRefIdOrderByCreatedAtDesc(
                DiscrepancyCreatedFrom.HANDOVER_REPORT,
                handoverReportId
        );
    }

    private void markDevicesForPostRentalQc(RentalOrder rentalOrder, List<HandoverReportItem> items) {
        if (rentalOrder == null || rentalOrder.getOrderId() == null) {
            return;
        }
        Set<Device> devices = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(items)) {
            items.stream()
                    .map(HandoverReportItem::getAllocation)
                    .filter(Objects::nonNull)
                    .map(Allocation::getDevice)
                    .filter(Objects::nonNull)
                    .forEach(devices::add);
        }
        if (devices.isEmpty()) {
            allocationRepository.findByOrderDetail_RentalOrder_OrderId(rentalOrder.getOrderId()).stream()
                    .map(Allocation::getDevice)
                    .filter(Objects::nonNull)
                    .forEach(devices::add);
        }
        if (devices.isEmpty()) {
            return;
        }
        devices.forEach(device -> device.setStatus(DeviceStatus.POST_RENTAL_QC));
        deviceRepository.saveAll(devices);
    }

    private List<HandoverReportItem> resolveItems(HandoverReportBaseCreateRequestDto request, RentalOrder rentalOrder) {
        return resolveItems(request.getItems(), rentalOrder, true);
    }

    private List<HandoverReportItem> resolveItems(List<HandoverReportItemRequestDto> itemDtos,
                                                  RentalOrder rentalOrder,
                                                  boolean fallbackToOrderDetails) {
        if (!CollectionUtils.isEmpty(itemDtos)) {
            return itemDtos.stream()
                    .filter(Objects::nonNull)
                    .map(dto -> mapDtoToItem(dto, rentalOrder))
                    .collect(Collectors.toList());
        }
        if (!fallbackToOrderDetails || rentalOrder == null || rentalOrder.getOrderId() == null) {
            return List.of();
        }
        List<OrderDetail> orderDetails = orderDetailRepository.findByRentalOrder_OrderId(rentalOrder.getOrderId());
        if (CollectionUtils.isEmpty(orderDetails)) {
            log.warn("No order details found for order {}", rentalOrder.getOrderId());
            return List.of();
        }
        return orderDetails.stream()
                .map(HandoverReportItem::fromOrderDetail)
                .collect(Collectors.toList());
    }

    private HandoverReportItem mapDtoToItem(HandoverReportItemRequestDto itemDto, RentalOrder rentalOrder) {
        HandoverReportItem entity = HandoverReportItem.builder().build();
        if (itemDto.getDeviceId() != null && rentalOrder != null && rentalOrder.getOrderId() != null) {
            Allocation allocation = allocationRepository
                    .findByOrderDetail_RentalOrder_OrderIdAndDevice_DeviceId(rentalOrder.getOrderId(), itemDto.getDeviceId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Allocation not found for order " + rentalOrder.getOrderId() + " và device " + itemDto.getDeviceId()));
            entity.setAllocation(allocation);
        }
        entity.setEvidenceUrls(resolveItemEvidence(itemDto.getEvidenceUrls()));
        return entity;
    }

    private List<String> resolveItemEvidence(List<String> evidences) {
        if (CollectionUtils.isEmpty(evidences)) {
            return new ArrayList<>();
        }
        return evidences.stream()
                .filter(url -> url != null && !url.isBlank())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void clearSnapshotsForOrder(Long orderId, HandoverType handoverType) {
        if (orderId == null || handoverType == null) {
            return;
        }
        AllocationSnapshotType snapshotType = handoverType == HandoverType.CHECKOUT
                ? AllocationSnapshotType.BASELINE
                : AllocationSnapshotType.FINAL;
        AllocationSnapshotSource source = handoverType == HandoverType.CHECKOUT
                ? AllocationSnapshotSource.HANDOVER_OUT
                : AllocationSnapshotSource.HANDOVER_IN;
        allocationRepository.findByOrderDetail_RentalOrder_OrderId(orderId).stream()
                .map(Allocation::getAllocationId)
                .filter(Objects::nonNull)
                .forEach(allocationId -> allocationConditionSnapshotRepository
                        .deleteByAllocation_AllocationIdAndSnapshotTypeAndSource(allocationId, snapshotType, source));
    }

    private void createHandoverSnapshots(List<HandoverReportItem> items, HandoverType handoverType, Staff staff) {
        if (handoverType == null || CollectionUtils.isEmpty(items)) {
            return;
        }
        AllocationSnapshotType snapshotType = handoverType == HandoverType.CHECKOUT
                ? AllocationSnapshotType.BASELINE
                : AllocationSnapshotType.FINAL;
        AllocationSnapshotSource source = handoverType == HandoverType.CHECKOUT
                ? AllocationSnapshotSource.HANDOVER_OUT
                : AllocationSnapshotSource.HANDOVER_IN;
        List<Allocation> allocations = items.stream()
                .map(HandoverReportItem::getAllocation)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        allocationSnapshotService.createSnapshots(allocations, snapshotType, source, staff);
    }

    private void createSnapshotsFromDeviceConditions(HandoverReport report,
                                                     List<HandoverDeviceConditionRequestDto> deviceConditions,
                                                     Staff staff) {
        if (report == null || report.getRentalOrder() == null
                || CollectionUtils.isEmpty(deviceConditions)) {
            return;
        }
        Long orderId = report.getRentalOrder().getOrderId();
        if (orderId == null) {
            return;
        }
        Set<Long> conditionDefinitionIds = deviceConditions.stream()
                .filter(Objects::nonNull)
                .map(HandoverDeviceConditionRequestDto::getConditionDefinitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ConditionDefinition> conditionDefinitionsById = conditionDefinitionIds.isEmpty()
                ? Collections.emptyMap()
                : conditionDefinitionRepository.findAllById(conditionDefinitionIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ConditionDefinition::getConditionDefinitionId, Function.identity()));
        List<AllocationConditionSnapshot> snapshots = new ArrayList<>();
        for (HandoverDeviceConditionRequestDto condition : deviceConditions) {
            if (condition == null || condition.getDeviceId() == null || condition.getConditionDefinitionId() == null) {
                continue;
            }
            ConditionDefinition conditionDefinition = conditionDefinitionsById.get(condition.getConditionDefinitionId());
            allocationRepository.findByOrderDetail_RentalOrder_OrderIdAndDevice_DeviceId(orderId, condition.getDeviceId())
                    .ifPresentOrElse(allocation -> {
                        AllocationConditionSnapshot snapshot = AllocationConditionSnapshot.builder()
                                .allocation(allocation)
                                .snapshotType(AllocationSnapshotType.BASELINE)
                                .source(AllocationSnapshotSource.HANDOVER_OUT)
                                .conditionDetails(List.of(AllocationConditionDetail.builder()
                                        .conditionDefinitionId(condition.getConditionDefinitionId())
                                        .conditionDefinitionName(conditionDefinition != null ? conditionDefinition.getName() : null)
                                        .severity(condition.getSeverity())
                                        .build()))
                                .images(condition.getImages() == null ? new ArrayList<>() : new ArrayList<>(condition.getImages()))
                                .staff(staff)
                                .build();
                        snapshots.add(snapshot);
                    }, () -> log.warn("Không tìm thấy allocation cho device {} thuộc order {}", condition.getDeviceId(), orderId));
        }
        if (!snapshots.isEmpty()) {
            allocationConditionSnapshotRepository.saveAll(snapshots);
        }
    }

    private void replaceDiscrepancies(Long handoverReportId, List<DiscrepancyInlineRequestDto> discrepancies) {
        if (handoverReportId == null) {
            return;
        }
        List<DiscrepancyReport> existing = discrepancyReportRepository
                .findByCreatedFromAndRefIdOrderByCreatedAtDesc(DiscrepancyCreatedFrom.HANDOVER_REPORT, handoverReportId);
        if (!CollectionUtils.isEmpty(existing)) {
            discrepancyReportRepository.deleteAll(existing);
        }
        createDiscrepancies(discrepancies, handoverReportId);
    }

    private void createDiscrepancies(List<DiscrepancyInlineRequestDto> discrepancies, Long handoverReportId) {
        if (handoverReportId == null || discrepancies == null || discrepancies.isEmpty()) {
            return;
        }
        discrepancies.stream()
                .filter(Objects::nonNull)
                .forEach(dto -> discrepancyReportService.create(
                        DiscrepancyReportRequestDto.builder()
                                .createdFrom(DiscrepancyCreatedFrom.HANDOVER_REPORT)
                                .refId(handoverReportId)
                                .discrepancyType(dto.getDiscrepancyType())
                                .conditionDefinitionId(dto.getConditionDefinitionId())
                                .orderDetailId(dto.getOrderDetailId())
                                .deviceId(dto.getDeviceId())
                                .staffNote(dto.getStaffNote())
                                .customerNote(dto.getCustomerNote())
                                .build()
                ));
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
