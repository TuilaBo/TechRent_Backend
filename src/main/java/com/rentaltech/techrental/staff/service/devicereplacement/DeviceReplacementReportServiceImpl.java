package com.rentaltech.techrental.staff.service.devicereplacement;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.contract.service.EmailService;
import com.rentaltech.techrental.contract.service.SMSService;
import com.rentaltech.techrental.device.model.*;
import com.rentaltech.techrental.device.model.dto.DeviceConditionResponseDto;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportRequestDto;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportResponseDto;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import com.rentaltech.techrental.device.repository.DiscrepancyReportRepository;
import com.rentaltech.techrental.device.service.AllocationSnapshotService;
import com.rentaltech.techrental.device.service.DeviceConditionService;
import com.rentaltech.techrental.device.service.DiscrepancyReportService;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.staff.model.*;
import com.rentaltech.techrental.staff.model.dto.*;
import com.rentaltech.techrental.staff.repository.DeviceReplacementReportItemRepository;
import com.rentaltech.techrental.staff.repository.DeviceReplacementReportRepository;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.webapi.customer.model.ComplaintFaultSource;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.CustomerComplaint;
import com.rentaltech.techrental.webapi.customer.repository.CustomerComplaintRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceReplacementReportServiceImpl implements DeviceReplacementReportService {

    private final TaskRepository taskRepository;
    private final RentalOrderRepository rentalOrderRepository;
    private final DeviceReplacementReportRepository replacementReportRepository;
    private final DeviceReplacementReportItemRepository replacementReportItemRepository;
    private final ImageStorageService imageStorageService;
    private final AllocationRepository allocationRepository;
    private final SMSService smsService;
    private final EmailService emailService;
    private final AccountService accountService;
    private final StaffRepository staffRepository;
    private final AllocationSnapshotService allocationSnapshotService;
    private final CustomerComplaintRepository customerComplaintRepository;
    private final DiscrepancyReportService discrepancyReportService;
    private final DiscrepancyReportRepository discrepancyReportRepository;
    private final DeviceConditionService deviceConditionService;

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
    public DeviceReplacementReportResponseDto createDeviceReplacementReport(Long complaintId, String staffUsername) {
        return createDeviceReplacementReport(complaintId, staffUsername, null);
    }

    @Override
    @Transactional
    public DeviceReplacementReportResponseDto createDeviceReplacementReport(Long complaintId, String staffUsername, Task deviceReplacementTask) {
        CustomerComplaint complaint =
                customerComplaintRepository.findById(complaintId)
                        .orElseThrow(() -> new IllegalArgumentException("Customer complaint not found: " + complaintId));

        if (complaint.getStatus() != com.rentaltech.techrental.webapi.customer.model.ComplaintStatus.PROCESSING) {
            throw new IllegalStateException("Chỉ có thể tạo biên bản đổi thiết bị khi complaint ở trạng thái PROCESSING");
        }

        RentalOrder rentalOrder = complaint.getRentalOrder();
        if (rentalOrder == null) {
            throw new IllegalStateException("Complaint chưa có rental order");
        }

        // Ưu tiên dùng task "Device Replacement" được truyền vào, nếu không có thì tìm từ order
        Task task = deviceReplacementTask;
        if (task == null || task.getTaskId() == null) {
            // Tìm task "Device Replacement" của order này
            task = taskRepository.findByOrderId(rentalOrder.getOrderId()).stream()
                    .filter(t -> {
                        String categoryName = t.getTaskCategory() != null ? t.getTaskCategory().getName() : null;
                        return "Device Replacement".equalsIgnoreCase(categoryName)
                                && (t.getStatus() == TaskStatus.PENDING || t.getStatus() == TaskStatus.IN_PROGRESS);
                    })
                    .findFirst()
                    .orElse(null);

            // Nếu vẫn không tìm thấy, fallback về replacementTask của complaint (Pre rental QC Replace)
            if (task == null) {
                task = complaint.getReplacementTask();
                if (task == null || task.getTaskId() == null) {
                    throw new IllegalStateException("Complaint chưa có replacement task và không tìm thấy task Device Replacement cho order");
                }
                log.warn("Không tìm thấy task Device Replacement cho order {}, sử dụng task Pre rental QC Replace (taskId: {})",
                        rentalOrder.getOrderId(), task.getTaskId());
            }
        }

        Allocation oldAllocation = complaint.getAllocation();
        Allocation newAllocation = complaint.getReplacementAllocation();
        if (oldAllocation == null || newAllocation == null) {
            throw new IllegalStateException("Complaint thiếu thông tin allocation (cũ hoặc mới)");
        }

        Account staffAccount = accountService.getByUsername(staffUsername)
                .orElseThrow(() -> new IllegalArgumentException("Staff account not found: " + staffUsername));

        Staff createdByStaff = staffRepository.findByAccount_AccountId(staffAccount.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Staff not found for account: " + staffUsername));

        // OPTION 3: Tự động gộp vào biên bản pending nếu có
        // Tìm biên bản pending của order (chưa ký, có thể thêm items)
        Optional<DeviceReplacementReport> existingReportOpt = replacementReportRepository
                .findPendingReportByOrderId(rentalOrder.getOrderId(), DeviceReplacementReportStatus.PENDING_STAFF_SIGNATURE);

        DeviceReplacementReport report;
        boolean isNewReport = false;

        if (existingReportOpt.isPresent()) {
            // Đã có biên bản pending → thêm items vào biên bản đó
            report = existingReportOpt.get();
            log.info("Tìm thấy biên bản pending #{} cho order #{}, thêm items từ complaint #{}",
                    report.getReplacementReportId(), rentalOrder.getOrderId(), complaintId);
        } else {
            // Chưa có biên bản pending → tạo mới
            isNewReport = true;

            // Tạo customer info và technician info
            Customer customer = rentalOrder.getCustomer();
            String customerInfo = customer != null && customer.getAccount() != null
                    ? String.format("%s - %s - %s",
                    customer.getAccount().getUsername(),
                    customer.getAccount().getPhoneNumber(),
                    customer.getAccount().getEmail())
                    : "N/A";

            String technicianInfo = String.format("%s - %s",
                    staffAccount.getUsername(),
                    staffAccount.getEmail());

            report = DeviceReplacementReport.builder()
                    .task(task) // Link với task của complaint đầu tiên
                    .rentalOrder(rentalOrder)
                    .createdByStaff(createdByStaff)
                    .customerInfo(customerInfo)
                    .technicianInfo(technicianInfo)
                    .replacementDateTime(LocalDateTime.now())
                    .replacementLocation("Đổi thiết bị tại địa điểm khách hàng")
                    .status(DeviceReplacementReportStatus.PENDING_STAFF_SIGNATURE)
                    .staffSigned(false)
                    .customerSigned(false)
                    .items(new ArrayList<>())
                    .build();

            log.info("Tạo biên bản mới cho order #{}, complaint #{}", rentalOrder.getOrderId(), complaintId);
        }

        // Tạo items: device cũ và device mới
        DeviceReplacementReportItem oldItem = DeviceReplacementReportItem.builder()
                .replacementReport(report)
                .allocation(oldAllocation)
                .isOldDevice(true)
                .evidenceUrls(new ArrayList<>())
                .build();

        DeviceReplacementReportItem newItem = DeviceReplacementReportItem.builder()
                .replacementReport(report)
                .allocation(newAllocation)
                .isOldDevice(false)
                .evidenceUrls(new ArrayList<>())
                .build();

        // Thêm items vào biên bản
        if (report.getItems() == null) {
            report.setItems(new ArrayList<>());
        }
        report.getItems().add(oldItem);
        report.getItems().add(newItem);

        DeviceReplacementReport saved = replacementReportRepository.save(report);
        replacementReportItemRepository.saveAll(List.of(oldItem, newItem));
        // KHÔNG tạo DiscrepancyReport ở đây vì chưa có conditionDefinitionIds
        // DiscrepancyReport sẽ được tạo khi gọi API fault với conditionDefinitionIds cụ thể
        // createDiscrepancyForOldDevice(complaint, oldAllocation, null);

        // Tạo snapshots cho cả 2 devices
        // Device cũ: FINAL snapshot (trước khi đổi)
        // Device mới: BASELINE snapshot (sau khi đổi)
        List<Allocation> oldAllocations = List.of(oldAllocation);
        List<Allocation> newAllocations = List.of(newAllocation);

        allocationSnapshotService.createSnapshots(oldAllocations,
                AllocationSnapshotType.FINAL,
                AllocationSnapshotSource.HANDOVER_IN,
                createdByStaff);

        allocationSnapshotService.createSnapshots(newAllocations,
                AllocationSnapshotType.BASELINE,
                AllocationSnapshotSource.HANDOVER_OUT,
                createdByStaff);

        // Chỉ gửi PIN khi tạo biên bản mới (không gửi lại khi thêm items)
        if (isNewReport) {
            String pinCode = generatePinCode();
            String staffEmail = staffAccount.getEmail();
            if (!StringUtils.hasText(staffEmail)) {
                throw new IllegalStateException("Staff account does not have email to receive PIN");
            }
            boolean emailSent = emailService.sendDeviceReplacementOTP(staffEmail, pinCode);
            if (!emailSent) {
                throw new IllegalStateException("Unable to send PIN to staff email: " + staffEmail);
            }
            savePinCode(buildPinKeyForReport(saved.getReplacementReportId()), pinCode);
        }

        // Link replacement report với complaint
        complaint.setReplacementReport(saved);
        customerComplaintRepository.save(complaint);

        return buildReplacementReportResponseDto(saved);
    }

    @Override
    @Transactional
    public DeviceReplacementReportResponseDto getReport(Long replacementReportId) {
        DeviceReplacementReport report = replacementReportRepository.findById(replacementReportId)
                .orElseThrow(() -> new IllegalArgumentException("Device replacement report not found: " + replacementReportId));
        return buildReplacementReportResponseDto(report);
    }

    @Override
    @Transactional
    public List<DeviceReplacementReportResponseDto> getReportsByTask(Long taskId) {
        return replacementReportRepository.findByTask_TaskId(taskId).stream()
                .map(this::buildReplacementReportResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<DeviceReplacementReportResponseDto> getReportsByOrder(Long orderId) {
        return replacementReportRepository.findByRentalOrder_OrderId(orderId).stream()
                .map(this::buildReplacementReportResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<DeviceReplacementReportResponseDto> getReportsByStaff(Long staffId) {
        return replacementReportRepository.findByStaff(staffId).stream()
                .map(this::buildReplacementReportResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<DeviceReplacementReportResponseDto> getAllReports() {
        return replacementReportRepository.findAll().stream()
                .map(this::buildReplacementReportResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HandoverPinDeliveryDto sendPinToStaffForReport(Long replacementReportId) {
        DeviceReplacementReport report = replacementReportRepository.findById(replacementReportId)
                .orElseThrow(() -> new IllegalArgumentException("Device replacement report not found: " + replacementReportId));

        if (report.getStatus() != DeviceReplacementReportStatus.PENDING_STAFF_SIGNATURE) {
            throw new IllegalStateException("Report is not in PENDING_STAFF_SIGNATURE status");
        }

        Task task = report.getTask();
        if (task == null || task.getAssignedStaff() == null || task.getAssignedStaff().isEmpty()) {
            throw new IllegalStateException("Task không có staff được assign");
        }

        Staff staff = task.getAssignedStaff().iterator().next();
        Account staffAccount = staff.getAccount();
        if (staffAccount == null || !StringUtils.hasText(staffAccount.getEmail())) {
            throw new IllegalStateException("Staff account does not have email");
        }

        String pinCode = generatePinCode();
        boolean emailSent = emailService.sendDeviceReplacementOTP(staffAccount.getEmail(), pinCode);
        if (!emailSent) {
            throw new IllegalStateException("Unable to send PIN to staff email: " + staffAccount.getEmail());
        }

        savePinCode(buildPinKeyForReport(replacementReportId), pinCode);

        return HandoverPinDeliveryDto.builder()
                .orderId(report.getRentalOrder().getOrderId())
                .email(staffAccount.getEmail())
                .emailSent(true)
                .build();
    }

    @Override
    @Transactional
    public DeviceReplacementReportResponseDto signByStaff(Long replacementReportId, DeviceReplacementReportStaffSignRequestDto request) {
        DeviceReplacementReport report = replacementReportRepository.findById(replacementReportId)
                .orElseThrow(() -> new IllegalArgumentException("Device replacement report not found: " + replacementReportId));

        if (report.getStatus() != DeviceReplacementReportStatus.PENDING_STAFF_SIGNATURE) {
            throw new IllegalStateException("Report is not in PENDING_STAFF_SIGNATURE status");
        }

        String key = buildPinKeyForReport(replacementReportId);
        String expectedPin = getPinCode(key);
        if (expectedPin == null || !expectedPin.equals(request.getPin())) {
            throw new IllegalArgumentException("Mã PIN không hợp lệ hoặc đã hết hạn");
        }

        report.setStaffSigned(true);
        report.setStaffSignedAt(LocalDateTime.now());
        report.setStaffSignature(request.getSignature());
        report.setStatus(DeviceReplacementReportStatus.STAFF_SIGNED);

        DeviceReplacementReport saved = replacementReportRepository.save(report);
        deletePinCode(key);

        // Dùng buildReplacementReportResponseDto để đảm bảo có faultSource và discrepancies mới nhất
        return buildReplacementReportResponseDto(saved);
    }

    @Override
    @Transactional
    public HandoverPinDeliveryDto sendPinToCustomerForReport(Long replacementReportId, String email) {
        DeviceReplacementReport report = replacementReportRepository.findById(replacementReportId)
                .orElseThrow(() -> new IllegalArgumentException("Device replacement report not found: " + replacementReportId));

        if (report.getStatus() != DeviceReplacementReportStatus.STAFF_SIGNED) {
            throw new IllegalStateException("Report must be signed by staff before customer can sign");
        }

        if (report.getCustomerSigned()) {
            throw new IllegalStateException("Report has already been signed by customer");
        }

        RentalOrder order = report.getRentalOrder();
        if (order == null || order.getCustomer() == null) {
            throw new IllegalStateException("Cannot determine customer for report");
        }

        Customer customer = order.getCustomer();
        String customerEmail = email != null ? email : (customer.getAccount() != null ? customer.getAccount().getEmail() : null);
        String phone = customer.getAccount() != null ? customer.getAccount().getPhoneNumber() : null;

        if (!StringUtils.hasText(customerEmail)) {
            throw new IllegalStateException("Customer email is required to send PIN");
        }

        String pinCode = generatePinCode();
        boolean smsSent = false;
        boolean emailSent = false;

        if (StringUtils.hasText(phone)) {
            smsSent = smsService.sendOTP(phone, pinCode);
        }

        emailSent = emailService.sendDeviceReplacementOTP(customerEmail, pinCode);
        if (!emailSent) {
            throw new IllegalStateException("Unable to send PIN to customer email: " + customerEmail);
        }

        savePinCode(buildPinKeyForReport(replacementReportId), pinCode);

        return HandoverPinDeliveryDto.builder()
                .orderId(order.getOrderId())
                .customerId(customer.getCustomerId())
                .customerName(customer.getAccount() != null ? customer.getAccount().getUsername() : null)
                .phoneNumber(phone)
                .email(customerEmail)
                .smsSent(smsSent)
                .emailSent(emailSent)
                .build();
    }

    @Override
    @Transactional
    public DeviceReplacementReportResponseDto signByCustomer(Long replacementReportId, DeviceReplacementReportCustomerSignRequestDto request) {
        DeviceReplacementReport report = replacementReportRepository.findById(replacementReportId)
                .orElseThrow(() -> new IllegalArgumentException("Device replacement report not found: " + replacementReportId));

        if (report.getStatus() != DeviceReplacementReportStatus.STAFF_SIGNED) {
            throw new IllegalStateException("Report must be signed by staff before customer can sign");
        }

        if (report.getCustomerSigned()) {
            throw new IllegalStateException("Report has already been signed by customer");
        }

        String key = buildPinKeyForReport(replacementReportId);
        String expectedPin = getPinCode(key);
        if (expectedPin == null || !expectedPin.equals(request.getPin())) {
            throw new IllegalArgumentException("Mã PIN không hợp lệ hoặc đã hết hạn");
        }

        report.setCustomerSigned(true);
        report.setCustomerSignedAt(LocalDateTime.now());
        report.setCustomerSignature(request.getSignature());
        report.setStatus(DeviceReplacementReportStatus.BOTH_SIGNED);

        DeviceReplacementReport saved = replacementReportRepository.save(report);
        deletePinCode(key);

        // Tự động đánh dấu task "Device Replacement" là hoàn thành khi cả staff và customer đã ký
        markDeviceReplacementTaskCompleted(saved);

        // Dùng buildReplacementReportResponseDto để đảm bảo có faultSource và discrepancies mới nhất
        return buildReplacementReportResponseDto(saved);
    }

    @Override
    @Transactional
    public List<DeviceReplacementReportResponseDto> getReportsByCustomerOrder(Long customerId) {
        return replacementReportRepository.findByRentalOrder_Customer_CustomerId(customerId).stream()
                .map(this::buildReplacementReportResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Build DeviceReplacementReportResponseDto với đầy đủ thông tin về faultSource và discrepancies
     */
    private DeviceReplacementReportResponseDto buildReplacementReportResponseDto(DeviceReplacementReport report) {
        if (report == null) {
            return null;
        }

        // Lấy faultSource từ complaint (nếu có)
        ComplaintFaultSource faultSource = null;
        List<DiscrepancyReportResponseDto> discrepancies = Collections.emptyList();

        if (report.getRentalOrder() != null && report.getRentalOrder().getOrderId() != null) {
            // Tìm complaint liên quan đến report này
            List<CustomerComplaint> complaints = customerComplaintRepository
                    .findByRentalOrder_OrderId(report.getRentalOrder().getOrderId());

            if (complaints != null && !complaints.isEmpty()) {
                // Lấy faultSource từ complaint đầu tiên (hoặc có thể merge nếu có nhiều)
                CustomerComplaint complaint = complaints.stream()
                        .filter(c -> c.getReplacementReport() != null
                                && c.getReplacementReport().getReplacementReportId() != null
                                && c.getReplacementReport().getReplacementReportId().equals(report.getReplacementReportId()))
                        .findFirst()
                        .orElse(complaints.get(0));

                faultSource = complaint.getFaultSource();

                // Lấy DiscrepancyReport từ complaint
                if (complaint.getComplaintId() != null) {
                    try {
                        discrepancies = discrepancyReportService.getByReference(
                                DiscrepancyCreatedFrom.CUSTOMER_COMPLAINT,
                                complaint.getComplaintId()
                        );
                    } catch (Exception ex) {
                        // Log nhưng không throw
                    }
                }
            }
        }

        return DeviceReplacementReportResponseDto.fromEntity(report, faultSource, discrepancies);
    }

    @Override
    @Transactional
    public DeviceReplacementReportItemResponseDto updateEvidenceByDevice(Long replacementReportId, Long deviceId, List<MultipartFile> files, String staffUsername) {
        if (replacementReportId == null || deviceId == null) {
            throw new IllegalArgumentException("replacementReportId và deviceId không được để trống");
        }
        if (CollectionUtils.isEmpty(files)) {
            throw new IllegalArgumentException("Danh sách file không được để trống");
        }

        accountService.getByUsername(staffUsername)
                .orElseThrow(() -> new IllegalArgumentException("Staff account not found: " + staffUsername));

        DeviceReplacementReport report = replacementReportRepository.findById(replacementReportId)
                .orElseThrow(() -> new IllegalArgumentException("Device replacement report not found: " + replacementReportId));

        DeviceReplacementReportItem item = report.getItems().stream()
                .filter(i -> i.getAllocation() != null
                        && i.getAllocation().getDevice() != null
                        && deviceId.equals(i.getAllocation().getDevice().getDeviceId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy device trong biên bản"));

        Task task = report.getTask();
        Long taskId = task != null ? task.getTaskId() : null;
        if (taskId == null) {
            throw new IllegalStateException("Report không gắn với task nên không thể tải hình");
        }

        List<String> newUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String label = "replacement-evidence-" + (item.getEvidenceUrls() == null ? 1 : item.getEvidenceUrls().size() + newUrls.size() + 1);
                String url = imageStorageService.uploadHandoverEvidence(file, taskId, label);
                newUrls.add(url);
            }
        }

        if (item.getEvidenceUrls() == null) {
            item.setEvidenceUrls(new ArrayList<>());
        }
        item.getEvidenceUrls().addAll(newUrls);
        DeviceReplacementReportItem saved = replacementReportItemRepository.save(item);
        return DeviceReplacementReportItemResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public void createDiscrepancyReportIfNeeded(Long complaintId, List<Long> conditionDefinitionIds) {
        if (complaintId == null) {
            log.warn("complaintId null, bỏ qua tạo DiscrepancyReport");
            return;
        }

        CustomerComplaint complaint = customerComplaintRepository.findById(complaintId)
                .orElse(null);
        if (complaint == null) {
            log.warn("Không tìm thấy complaint {} để tạo DiscrepancyReport", complaintId);
            return;
        }

        // Validate complaint có refId hợp lệ
        if (complaint.getComplaintId() == null || !complaint.getComplaintId().equals(complaintId)) {
            log.error("Complaint ID mismatch: expected {}, got {}", complaintId, complaint.getComplaintId());
            return;
        }

        Allocation allocation = complaint.getAllocation();
        if (allocation == null) {
            log.warn("Complaint {} không có allocation để tạo DiscrepancyReport", complaintId);
            return;
        }

        log.info("Bắt đầu tạo DiscrepancyReport cho complaint {} (allocation {}, conditionDefinitionIds: {})",
                complaintId, allocation.getAllocationId(), conditionDefinitionIds);
        createDiscrepancyForOldDevice(complaint, allocation, conditionDefinitionIds);
    }

    private void createDiscrepancyForOldDevice(CustomerComplaint complaint, Allocation allocation, List<Long> conditionDefinitionIds) {
        if (complaint == null || allocation == null) {
            log.warn("Bỏ qua tạo discrepancy: complaint hoặc allocation null");
            return;
        }
        if (complaint.getFaultSource() != ComplaintFaultSource.CUSTOMER) {
            log.debug("Bỏ qua tạo discrepancy cho complaint {} vì faultSource không phải CUSTOMER: {}",
                    complaint.getComplaintId(), complaint.getFaultSource());
            return;
        }
        Long complaintId = complaint.getComplaintId();
        Long orderDetailId = allocation.getOrderDetail() != null
                ? allocation.getOrderDetail().getOrderDetailId()
                : null;
        Long deviceId = allocation.getDevice() != null ? allocation.getDevice().getDeviceId() : null;

        if (complaintId == null) {
            log.error("Complaint không có complaintId, không thể tạo DiscrepancyReport");
            return;
        }
        if (orderDetailId == null) {
            log.error("Allocation không có orderDetailId, không thể tạo DiscrepancyReport cho complaint {}", complaintId);
            return;
        }
        if (deviceId == null) {
            log.error("Allocation không có deviceId, không thể tạo DiscrepancyReport cho complaint {}", complaintId);
            return;
        }

        log.debug("Tạo DiscrepancyReport: complaintId={}, orderDetailId={}, deviceId={}, conditionDefinitionIds={}",
                complaintId, orderDetailId, deviceId, conditionDefinitionIds);

        // XÓA TẤT CẢ DiscrepancyReports cũ của complaint này (với deviceId tương ứng) để đảm bảo response chỉ hiển thị những conditionDefinitionIds được gửi trong request hiện tại
        // Điều này cho phép cập nhật liên tục faultSource với conditionDefinitionIds mới
        List<DiscrepancyReport> existingReports = discrepancyReportRepository
                .findByCreatedFromAndRefIdOrderByCreatedAtDesc(DiscrepancyCreatedFrom.CUSTOMER_COMPLAINT, complaintId);

        if (existingReports != null && !existingReports.isEmpty()) {
            // Chỉ xóa DiscrepancyReports của device này (không xóa của device khác nếu có nhiều devices trong cùng complaint)
            List<DiscrepancyReport> reportsToDelete = existingReports.stream()
                    .filter(report -> report.getAllocation() != null
                            && report.getAllocation().getDevice() != null
                            && Objects.equals(report.getAllocation().getDevice().getDeviceId(), deviceId))
                    .collect(Collectors.toList());

            if (!reportsToDelete.isEmpty()) {
                log.info("🗑️ Xóa {} DiscrepancyReports cũ của complaint {} (device {})",
                        reportsToDelete.size(), complaintId, deviceId);
                discrepancyReportRepository.deleteAll(reportsToDelete);
                discrepancyReportRepository.flush(); // Flush để đảm bảo xóa hoàn tất trước khi tạo mới
            }
        }

        // Nếu không có conditionDefinitionIds được truyền vào, tạo DiscrepancyReport không có condition (penaltyAmount = 0)
        if (CollectionUtils.isEmpty(conditionDefinitionIds)) {
            String staffNote = StringUtils.hasText(complaint.getStaffNote())
                    ? complaint.getStaffNote()
                    : complaint.getCustomerDescription();
            try {
                discrepancyReportService.create(DiscrepancyReportRequestDto.builder()
                        .createdFrom(DiscrepancyCreatedFrom.CUSTOMER_COMPLAINT)
                        .refId(complaintId)
                        .discrepancyType(DiscrepancyType.DAMAGE)
                        .orderDetailId(orderDetailId)
                        .deviceId(deviceId)
                        .staffNote(staffNote)
                        .build());
                log.info("✅ Đã tạo DiscrepancyReport cho complaint {} (device {}, không có condition)",
                        complaintId, deviceId);
            } catch (Exception ex) {
                log.error("❌ Lỗi khi tạo DiscrepancyReport cho complaint {} (device {}, không có condition): {}",
                        complaintId, deviceId, ex.getMessage(), ex);
                throw new RuntimeException("Không thể tạo DiscrepancyReport cho complaint " + complaintId
                        + ": " + ex.getMessage(), ex);
            }
            return;
        }

        // Tạo DiscrepancyReport cho mỗi conditionDefinitionId được truyền vào
        // (Đã xóa tất cả DiscrepancyReports cũ ở trên, nên không cần check duplicate nữa)
        String staffNote = StringUtils.hasText(complaint.getStaffNote())
                ? complaint.getStaffNote()
                : complaint.getCustomerDescription();

        for (Long conditionDefinitionId : conditionDefinitionIds) {
            if (conditionDefinitionId == null) {
                continue;
            }

            // Tạo DiscrepancyReport với conditionDefinitionId
            // penaltyAmount sẽ được tính tự động từ ConditionDefinition.defaultCompensation
            try {
                discrepancyReportService.create(DiscrepancyReportRequestDto.builder()
                        .createdFrom(DiscrepancyCreatedFrom.CUSTOMER_COMPLAINT)
                        .refId(complaintId)
                        .discrepancyType(DiscrepancyType.DAMAGE)
                        .conditionDefinitionId(conditionDefinitionId)
                        .orderDetailId(orderDetailId)
                        .deviceId(deviceId)
                        .staffNote(staffNote)
                        .build());
                log.info("✅ Đã tạo DiscrepancyReport cho complaint {} (device {}, condition {})",
                        complaintId, deviceId, conditionDefinitionId);
            } catch (Exception ex) {
                log.error("❌ Lỗi khi tạo DiscrepancyReport cho complaint {} (device {}, condition {}): {}",
                        complaintId, deviceId, conditionDefinitionId, ex.getMessage(), ex);
                throw ex; // Re-throw để transaction rollback
            }
        }
    }


    // PIN management methods
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
            log.warn("Redis unavailable for replacement pin storage, fallback to in-memory cache: {}", ex.getMessage());
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
            log.warn("Redis unavailable for replacement pin retrieval: {}", ex.getMessage());
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
            log.warn("Redis unavailable for replacement pin deletion: {}", ex.getMessage());
        }
        pinCache.remove(key);
    }

    private void cleanupExpiredPins() {
        pinCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private String buildPinKeyForReport(Long replacementReportId) {
        return "replacement_pin_report_" + replacementReportId;
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

    /**
     * Tự động đánh dấu task "Device Replacement" là hoàn thành khi cả staff và customer đã ký biên bản
     */
    private void markDeviceReplacementTaskCompleted(DeviceReplacementReport report) {
        if (report == null || report.getRentalOrder() == null) {
            return;
        }

        RentalOrder order = report.getRentalOrder();
        Long orderId = order.getOrderId();

        // Tìm task "Device Replacement" cho order này
        List<Task> deliveryTasks = taskRepository.findByOrderId(orderId).stream()
                .filter(task -> {
                    if (task.getStatus() == TaskStatus.COMPLETED) {
                        return false; // Đã completed rồi, skip
                    }
                    String categoryName = task.getTaskCategory() != null ? task.getTaskCategory().getName() : null;
                    return "Device Replacement".equalsIgnoreCase(categoryName);
                })
                .collect(Collectors.toList());

        if (deliveryTasks.isEmpty()) {
            log.debug("Không tìm thấy task Device Replacement pending cho order {} để đánh dấu completed", orderId);
            return;
        }

        // Đánh dấu tất cả task "Device Replacement" pending là completed
        LocalDateTime now = LocalDateTime.now();
        for (Task task : deliveryTasks) {
            task.setStatus(TaskStatus.COMPLETED);
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(now);
            }
            taskRepository.save(task);
            log.info("Đã đánh dấu task Device Replacement (taskId: {}) là COMPLETED sau khi customer ký biên bản replacement (reportId: {})",
                    task.getTaskId(), report.getReplacementReportId());
        }
    }
}

