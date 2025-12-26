package com.rentaltech.techrental.webapi.customer.service.impl;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.model.DiscrepancyCreatedFrom;
import com.rentaltech.techrental.device.model.dto.DeviceConditionResponseDto;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportResponseDto;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.device.service.DiscrepancyReportService;
import com.rentaltech.techrental.rentalorder.model.BookingStatus;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.BookingCalendarRepository;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.rentalorder.service.BookingCalendarService;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.staff.service.taskservice.TaskService;
import com.rentaltech.techrental.webapi.customer.model.ComplaintFaultSource;
import com.rentaltech.techrental.webapi.customer.model.ComplaintStatus;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.CustomerComplaint;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerComplaintRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerComplaintResponseDto;
import com.rentaltech.techrental.webapi.customer.repository.CustomerComplaintCustomRepository;
import com.rentaltech.techrental.webapi.customer.repository.CustomerComplaintRepository;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.customer.service.CustomerComplaintService;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerComplaintServiceImpl implements CustomerComplaintService {

    private final CustomerComplaintRepository complaintRepository;
    private final CustomerComplaintCustomRepository complaintCustomRepository;
    private final CustomerRepository customerRepository;
    private final RentalOrderRepository rentalOrderRepository;
    private final DeviceRepository deviceRepository;
    private final AllocationRepository allocationRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final com.rentaltech.techrental.staff.repository.TaskCustomRepository taskCustomRepository;
    private final StaffService staffService;
    private final AccountService accountService;
    private final BookingCalendarService bookingCalendarService;
    private final BookingCalendarRepository bookingCalendarRepository;
    private final ImageStorageService imageStorageService;
    private final com.rentaltech.techrental.staff.service.devicereplacement.DeviceReplacementReportService deviceReplacementReportService;
    private final com.rentaltech.techrental.device.service.DeviceConditionService deviceConditionService;
    private final DiscrepancyReportService discrepancyReportService;

    @Override
    public CustomerComplaintResponseDto createComplaint(CustomerComplaintRequestDto request, MultipartFile evidenceImage, String username) {
        Customer customer = customerRepository.findByAccount_Username(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng"));

        RentalOrder order = rentalOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng: " + request.getOrderId()));

        // Validate order thuộc về customer
        if (!order.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("Không có quyền khiếu nại đơn hàng này");
        }

        // Validate order đang ở status IN_USE
        if (order.getOrderStatus() != OrderStatus.IN_USE) {
            throw new IllegalStateException("Chỉ có thể khiếu nại khi đơn hàng đang trong trạng thái IN_USE");
        }

        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thiết bị: " + request.getDeviceId()));

        // Validate device thuộc order (qua Allocation)
        Allocation allocation = allocationRepository.findByOrderDetail_RentalOrder_OrderIdAndDevice_DeviceId(
                        order.getOrderId(), device.getDeviceId())
                .orElseThrow(() -> new IllegalStateException("Thiết bị không thuộc đơn hàng này"));

        // Check xem đã có complaint pending chưa
        complaintCustomRepository.findByOrderIdAndDeviceId(order.getOrderId(), device.getDeviceId())
                .filter(c -> c.getStatus() == ComplaintStatus.PENDING || c.getStatus() == ComplaintStatus.PROCESSING)
                .ifPresent(c -> {
                    throw new IllegalStateException("Đã có khiếu nại đang xử lý cho thiết bị này");
                });

        CustomerComplaint complaint = CustomerComplaint.builder()
                .rentalOrder(order)
                .device(device)
                .allocation(allocation)
                .status(ComplaintStatus.PENDING)
                .customerDescription(request.getCustomerDescription())
                .build();

        CustomerComplaint saved = complaintRepository.save(complaint);

        // Upload ảnh bằng chứng nếu có
        if (evidenceImage != null && !evidenceImage.isEmpty()) {
            String evidenceUrl = imageStorageService.uploadComplaintEvidence(evidenceImage, saved.getComplaintId());
            saved.setEvidenceUrls(evidenceUrl);
            saved = complaintRepository.save(saved);
        }

        return buildComplaintResponseDto(saved);
    }

    /**
     * Build CustomerComplaintResponseDto với đầy đủ thông tin về hư hỏng và thiệt hại
     */
    private CustomerComplaintResponseDto buildComplaintResponseDto(CustomerComplaint complaint) {
        if (complaint == null) {
            return null;
        }

        // Lấy DiscrepancyReport từ complaint
        List<DiscrepancyReportResponseDto> discrepancies = Collections.emptyList();
        List<Long> conditionDefinitionIds = Collections.emptyList();
        String damageNote = null;

        if (complaint.getComplaintId() != null) {
            try {
                discrepancies = discrepancyReportService.getByReference(
                        DiscrepancyCreatedFrom.CUSTOMER_COMPLAINT,
                        complaint.getComplaintId()
                );

                // CHỈ lấy conditionDefinitionIds từ DiscrepancyReport để đảm bảo chỉ hiển thị
                // những condition được xác định trong complaint này, không bị ảnh hưởng bởi condition cũ của device
                // KHÔNG fallback vào DeviceCondition khi đã có DiscrepancyReport để tránh hiển thị condition không được report
                if (discrepancies != null && !discrepancies.isEmpty()) {
                    conditionDefinitionIds = discrepancies.stream()
                            .map(DiscrepancyReportResponseDto::getConditionDefinitionId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());

                    // Lấy damageNote từ DiscrepancyReport đầu tiên
                    damageNote = discrepancies.stream()
                            .map(DiscrepancyReportResponseDto::getStaffNote)
                            .filter(Objects::nonNull)
                            .filter(note -> !note.isBlank())
                            .findFirst()
                            .orElse(null);
                }
            } catch (Exception ex) {
                // Log nhưng không throw để không làm gián đoạn flow
                log.warn("Lỗi khi lấy DiscrepancyReport cho complaint {}: {}",
                        complaint.getComplaintId(), ex.getMessage());
            }
        }

        // Fallback: CHỈ lấy từ DeviceCondition khi KHÔNG có DiscrepancyReport (faultSource = COMPANY hoặc UNKNOWN)
        // và CHỈ khi conditionDefinitionIds vẫn còn empty (chưa có DiscrepancyReport nào)
        if (conditionDefinitionIds.isEmpty() && complaint.getDevice() != null && complaint.getDevice().getDeviceId() != null) {
            List<DeviceConditionResponseDto> deviceConditions = deviceConditionService.getByDevice(complaint.getDevice().getDeviceId());
            if (deviceConditions != null && !deviceConditions.isEmpty()) {
                // Chỉ lấy condition được tạo sau khi complaint được created hoặc processed
                // để tránh lấy condition cũ từ đơn hàng trước
                LocalDateTime filterTime = complaint.getProcessedAt() != null
                        ? complaint.getProcessedAt()
                        : complaint.getCreatedAt();

                if (filterTime != null) {
                    deviceConditions = deviceConditions.stream()
                            .filter(dc -> dc.getCapturedAt() != null &&
                                    (dc.getCapturedAt().isAfter(filterTime) ||
                                            dc.getCapturedAt().isEqual(filterTime)))
                            .collect(Collectors.toList());
                }

                if (!deviceConditions.isEmpty()) {
                    conditionDefinitionIds = deviceConditions.stream()
                            .map(DeviceConditionResponseDto::getConditionDefinitionId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());

                    // Lấy damageNote từ condition đầu tiên
                    damageNote = deviceConditions.stream()
                            .map(DeviceConditionResponseDto::getNote)
                            .filter(Objects::nonNull)
                            .filter(note -> !note.isBlank())
                            .findFirst()
                            .orElse(null);
                }
            }
        }

        return CustomerComplaintResponseDto.from(complaint, conditionDefinitionIds, damageNote, discrepancies);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerComplaintResponseDto> getMyComplaints(String username) {
        Customer customer = customerRepository.findByAccount_Username(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng"));

        List<CustomerComplaint> complaints = complaintCustomRepository.findByCustomerId(customer.getCustomerId());
        return complaints.stream()
                .map(this::buildComplaintResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerComplaintResponseDto getComplaintById(Long complaintId, String username) {
        Customer customer = customerRepository.findByAccount_Username(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng"));

        CustomerComplaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khiếu nại: " + complaintId));

        // Validate complaint thuộc về customer
        if (!complaint.getRentalOrder().getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("Không có quyền xem khiếu nại này");
        }

        return buildComplaintResponseDto(complaint);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerComplaintResponseDto> getComplaintsByOrder(Long orderId, String username) {
        Customer customer = customerRepository.findByAccount_Username(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng"));

        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng: " + orderId));

        if (!order.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("Không có quyền xem khiếu nại của đơn hàng này");
        }

        List<CustomerComplaint> complaints = complaintRepository.findByRentalOrder_OrderId(orderId);
        return complaints.stream()
                .map(this::buildComplaintResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerComplaintResponseDto> getAllComplaints(ComplaintStatus status) {
        List<CustomerComplaint> complaints = status != null
                ? complaintRepository.findByStatus(status)
                : complaintRepository.findAll();
        return complaints.stream()
                .map(CustomerComplaintResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerComplaintResponseDto getComplaintById(Long complaintId) {
        CustomerComplaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khiếu nại: " + complaintId));
        return buildComplaintResponseDto(complaint);
    }

    @Override
    public CustomerComplaintResponseDto processComplaint(Long complaintId,
                                                         com.rentaltech.techrental.webapi.customer.model.ComplaintFaultSource faultSource,
                                                         java.util.List<Long> conditionDefinitionIds,
                                                         String damageNote,
                                                         String staffNote,
                                                         String username) {
        CustomerComplaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khiếu nại: " + complaintId));

        if (complaint.getStatus() != ComplaintStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể xử lý khiếu nại ở trạng thái PENDING");
        }

        Account account = accountService.getByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản"));
        Staff staff = staffService.getStaffByAccountIdOrThrow(account.getAccountId());

        RentalOrder order = complaint.getRentalOrder();
        Device brokenDevice = complaint.getDevice();
        Allocation oldAllocation = complaint.getAllocation();

        // Tìm OrderDetail tương ứng
        OrderDetail orderDetail = oldAllocation.getOrderDetail();
        if (orderDetail == null) {
            throw new IllegalStateException("Không tìm thấy OrderDetail cho allocation");
        }

        // Tìm device thay thế (cùng model với broken device, available trong toàn bộ khoảng thời gian của order)
        // QUAN TRỌNG: Dùng model từ broken device, không phải OrderDetail
        // Vì replacement device phải cùng model với broken device, không nhất thiết cùng model với OrderDetail
        if (brokenDevice.getDeviceModel() == null || brokenDevice.getDeviceModel().getDeviceModelId() == null) {
            throw new IllegalStateException("Broken device không có model. Không thể tìm device thay thế.");
        }
        Device replacementDevice = findReplacementDevice(
                brokenDevice.getDeviceModel().getDeviceModelId(),  // ← Dùng model từ broken device
                order.getEffectiveStartDate(),
                order.getEffectiveEndDate()
        );

        if (replacementDevice == null) {
            throw new IllegalStateException("Không tìm thấy thiết bị thay thế khả dụng. Vui lòng liên hệ bộ phận hỗ trợ.");
        }

        // Update device cũ: mark as DAMAGED
        brokenDevice.setStatus(DeviceStatus.DAMAGED);
        deviceRepository.save(brokenDevice);

        // Update device mới: mark as PRE_RENTAL_QC (chờ QC check trước khi allocation)
        replacementDevice.setStatus(DeviceStatus.PRE_RENTAL_QC);
        deviceRepository.save(replacementDevice);

        // Tìm hoặc tạo TaskCategory "Pre rental QC Replace" (riêng cho complaint, phân biệt với QC ban đầu)
        TaskCategory qcReplaceCategory = taskCategoryRepository.findByName("Pre rental QC Replace")
                .orElseGet(() -> {
                    // Tạo category mới nếu chưa có
                    TaskCategory newCategory = TaskCategory.builder()
                            .name("Pre rental QC Replace")
                            .description("Pre-Rental QC cho device thay thế khi có complaint")
                            .build();
                    return taskCategoryRepository.save(newCategory);
                });

        // Luôn tạo task mới với category "Pre rental QC Replace" cho complaint
        // Không tái sử dụng task QC ban đầu để phân biệt rõ ràng
        TaskCreateRequestDto qcTaskRequest = TaskCreateRequestDto.builder()
                .taskCategoryId(qcReplaceCategory.getTaskCategoryId())
                .orderId(order.getOrderId())
                .assignedStaffIds(null) // Không assign cho ai, để operator assign sau
                .description(String.format("Pre-Rental QC cho device thay thế (Complaint #%d). Device hỏng: %s (Serial: %s). Device đề xuất: %s (Serial: %s). Technician có thể chọn device khác cùng model khi QC.",
                        complaintId,
                        brokenDevice.getDeviceModel().getDeviceName(),
                        brokenDevice.getSerialNumber(),
                        replacementDevice.getDeviceModel().getDeviceName(),
                        replacementDevice.getSerialNumber()))
                .plannedStart(LocalDateTime.now())
                .plannedEnd(LocalDateTime.now().plusHours(6)) // Deadline 6h cho QC
                .build();
        Task qcTask = taskService.createTask(qcTaskRequest, username);

        // Update complaint
        // Lưu suggested device (technician có thể chọn device khác khi QC)
        complaint.setStatus(ComplaintStatus.PROCESSING);
        complaint.setFaultSource(faultSource != null
                ? faultSource
                : com.rentaltech.techrental.webapi.customer.model.ComplaintFaultSource.UNKNOWN);
        complaint.setStaffNote(staffNote);
        complaint.setReplacementDevice(replacementDevice); // Device đề xuất (có thể thay đổi sau khi QC)
        complaint.setReplacementTask(qcTask); // Task Pre-Rental QC
        complaint.setReplacementAllocation(null); // Chưa có allocation, sẽ được tạo sau khi QC pass
        complaint.setProcessedAt(LocalDateTime.now());

        // Ghi nhận condition hiện tại của thiết bị để cập nhật trạng thái
        if (conditionDefinitionIds != null && !conditionDefinitionIds.isEmpty()) {
            addDamageConditions(brokenDevice.getDeviceId(), conditionDefinitionIds, damageNote, staff.getStaffId());
        }

        CustomerComplaint saved = complaintRepository.save(complaint);
        complaintRepository.flush(); // Flush để đảm bảo complaint được save trước khi query

        // KHÔNG tạo biên bản ngay lúc này vì chưa có allocation
        // Biên bản sẽ được tạo sau khi QC pass và allocation được tạo

        return buildComplaintResponseDto(saved);
    }

    @Override
    public CustomerComplaintResponseDto updateFaultAndConditions(Long complaintId,
                                                                 com.rentaltech.techrental.webapi.customer.model.ComplaintFaultSource faultSource,
                                                                 java.util.List<Long> conditionDefinitionIds,
                                                                 String damageNote,
                                                                 String staffNote,
                                                                 String username) {
        CustomerComplaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khiếu nại: " + complaintId));

        if (complaint.getStatus() != ComplaintStatus.PROCESSING) {
            throw new IllegalStateException("Chỉ có thể cập nhật nguồn lỗi khi complaint ở trạng thái PROCESSING");
        }

        Account account = accountService.getByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản"));
        Staff staff = staffService.getStaffByAccountIdOrThrow(account.getAccountId());

        Device brokenDevice = complaint.getDevice();
        if (brokenDevice == null || brokenDevice.getDeviceId() == null) {
            throw new IllegalStateException("Không xác định được thiết bị của khiếu nại");
        }

        if (faultSource != null) {
            complaint.setFaultSource(faultSource);
        }
        if (staffNote != null && !staffNote.isBlank()) {
            complaint.setStaffNote(staffNote);
        }

        if (conditionDefinitionIds != null && !conditionDefinitionIds.isEmpty()) {
            addDamageConditions(brokenDevice.getDeviceId(), conditionDefinitionIds, damageNote, staff.getStaffId());
        }

        CustomerComplaint saved = complaintRepository.save(complaint);

        // Nếu faultSource = CUSTOMER, tạo DiscrepancyReport để tính phí thiệt hại
        // Được gọi sau khi technician xác định faultSource và conditionDefinitionIds
        // CHỈ tạo DiscrepancyReport cho những conditionDefinitionIds được truyền vào, không phải tất cả conditions của device
        if (saved.getFaultSource() == ComplaintFaultSource.CUSTOMER) {
            // Flush để đảm bảo complaint đã được persist trước khi tạo DiscrepancyReport
            // Check constraint có thể yêu cầu ref_id phải tồn tại trong customer_complaint
            complaintRepository.flush();

            try {
                log.info("🔄 Tạo DiscrepancyReport cho complaint {} với conditionDefinitionIds: {}",
                        saved.getComplaintId(), conditionDefinitionIds);
                deviceReplacementReportService.createDiscrepancyReportIfNeeded(saved.getComplaintId(), conditionDefinitionIds);
                log.info("✅ Đã tạo DiscrepancyReport cho complaint {} thành công", saved.getComplaintId());
            } catch (Exception ex) {
                // Log lỗi chi tiết để debug
                log.error("❌ Lỗi khi tạo DiscrepancyReport cho complaint {} với conditionDefinitionIds {}: {}",
                        saved.getComplaintId(), conditionDefinitionIds, ex.getMessage(), ex);
                // Không throw để không làm gián đoạn flow updateFaultAndConditions
                // DiscrepancyReport có thể được tạo sau khi replacement report được tạo
            }
        }

        return buildComplaintResponseDto(saved);
    }

    private void addDamageConditions(Long deviceId,
                                     java.util.List<Long> conditionDefinitionIds,
                                     String damageNote,
                                     Long staffId) {
        for (Long conditionDefinitionId : conditionDefinitionIds) {
            if (conditionDefinitionId == null) {
                continue;
            }
            deviceConditionService.updateCondition(
                    deviceId,
                    conditionDefinitionId,
                    null,
                    damageNote,
                    List.of(),
                    staffId);
        }
    }

    /**
     * Update task description để bao gồm device mới được thêm vào
     */
    private void updateTaskDescriptionWithNewDevice(Task task, Device brokenDevice, Device replacementDevice, Long complaintId) {
        if (task == null || task.getTaskId() == null) {
            return;
        }

        // Lấy tất cả complaints đang PROCESSING của order để build description đầy đủ
        List<CustomerComplaint> processingComplaints = complaintRepository
                .findByRentalOrder_OrderIdAndStatus(task.getOrderId(), ComplaintStatus.PROCESSING);

        if (processingComplaints.isEmpty()) {
            return;
        }

        // Build description với tất cả devices cần thay thế
        StringBuilder description = new StringBuilder();
        description.append(String.format("Thay thế thiết bị cho đơn hàng #%d:\n", task.getOrderId()));

        for (CustomerComplaint c : processingComplaints) {
            if (c.getDevice() != null && c.getReplacementDevice() != null) {
                description.append(String.format("- Khiếu nại #%d: %s (Serial: %s) → %s (Serial: %s)\n",
                        c.getComplaintId(),
                        c.getDevice().getDeviceModel() != null ? c.getDevice().getDeviceModel().getDeviceName() : "N/A",
                        c.getDevice().getSerialNumber(),
                        c.getReplacementDevice().getDeviceModel() != null ? c.getReplacementDevice().getDeviceModel().getDeviceName() : "N/A",
                        c.getReplacementDevice().getSerialNumber()));
            }
        }

        description.append("Vui lòng assign staff đi giao máy.");

        // Truncate nếu quá dài (description có limit 1000 chars)
        String finalDescription = description.length() > 1000
                ? description.substring(0, 997) + "..."
                : description.toString();

        task.setDescription(finalDescription);
        taskRepository.save(task);
    }

    @Override
    public CustomerComplaintResponseDto resolveComplaint(Long complaintId, String staffNote, List<MultipartFile> evidenceFiles, String username) {
        CustomerComplaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khiếu nại: " + complaintId));

        if (complaint.getStatus() != ComplaintStatus.PROCESSING) {
            throw new IllegalStateException("Chỉ có thể đánh dấu giải quyết khiếu nại ở trạng thái PROCESSING");
        }

        Account account = accountService.getByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản"));
        Staff staff = staffService.getStaffByAccountIdOrThrow(account.getAccountId());

        // Validate: Chỉ staff được assign task mới có thể resolve
        Task replacementTask = complaint.getReplacementTask();
        if (replacementTask != null && replacementTask.getTaskId() != null) {
            boolean isAssigned = replacementTask.getAssignedStaff() != null
                    && replacementTask.getAssignedStaff().stream()
                    .anyMatch(s -> s.getStaffId() != null && s.getStaffId().equals(staff.getStaffId()));
            // ADMIN và OPERATOR có thể resolve bất kỳ complaint nào
            boolean isAdminOrOperator = account.getRole() == com.rentaltech.techrental.authentication.model.Role.ADMIN
                    || account.getRole() == com.rentaltech.techrental.authentication.model.Role.OPERATOR;
            if (!isAssigned && !isAdminOrOperator) {
                throw new AccessDeniedException("Chỉ staff được assign task mới có thể đánh dấu giải quyết khiếu nại này");
            }
        }

        // Biên bản đã được tạo trong processComplaint, chỉ cần validate
        com.rentaltech.techrental.staff.model.DeviceReplacementReport replacementReport = complaint.getReplacementReport();
        if (replacementReport == null || replacementReport.getReplacementReportId() == null) {
            throw new IllegalStateException("Biên bản đổi thiết bị chưa được tạo. Vui lòng xử lý khiếu nại trước.");
        }
        if (!replacementReport.getStaffSigned() || !replacementReport.getCustomerSigned()) {
            throw new IllegalStateException("Cần cả nhân viên và khách hàng ký biên bản đổi thiết bị trước khi giải quyết khiếu nại");
        }

        // Upload ảnh bằng chứng
        List<String> evidenceUrls = new ArrayList<>();
        if (evidenceFiles != null && !evidenceFiles.isEmpty()) {
            for (MultipartFile file : evidenceFiles) {
                if (file != null && !file.isEmpty()) {
                    String url = imageStorageService.uploadComplaintEvidence(file, complaintId);
                    evidenceUrls.add(url);
                }
            }
        }

        // Update complaint
        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResolvedAt(LocalDateTime.now());
        complaint.setResolvedBy(staff);
        if (staffNote != null && !staffNote.isBlank()) {
            complaint.setStaffNote(staffNote);
        }
        if (!evidenceUrls.isEmpty()) {
            // Lưu danh sách URL dưới dạng JSON array string
            complaint.setEvidenceUrls(String.join(",", evidenceUrls));
        }
        // Đảm bảo replacementReport được link
        if (complaint.getReplacementReport() == null && replacementReport != null) {
            complaint.setReplacementReport(replacementReport);
        }

        CustomerComplaint saved = complaintRepository.save(complaint);

        // Tự động update task "Pre rental QC" status thành COMPLETED
        if (replacementTask != null && replacementTask.getTaskId() != null) {
            replacementTask.setStatus(TaskStatus.COMPLETED);
            replacementTask.setCompletedAt(LocalDateTime.now());
            taskRepository.save(replacementTask);
        }

        // Tự động đánh dấu task "Device Replacement" là hoàn thành (nếu chưa complete)
        markDeviceReplacementTaskCompleted(saved);

        return buildComplaintResponseDto(saved);
    }

    @Override
    public CustomerComplaintResponseDto cancelComplaint(Long complaintId, String staffNote, String username) {
        CustomerComplaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khiếu nại: " + complaintId));

        if (complaint.getStatus() == ComplaintStatus.RESOLVED || complaint.getStatus() == ComplaintStatus.CANCELLED) {
            throw new IllegalStateException("Không thể hủy khiếu nại ở trạng thái này");
        }

        Account account = accountService.getByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản"));
        Staff staff = staffService.getStaffByAccountIdOrThrow(account.getAccountId());

        complaint.setStatus(ComplaintStatus.CANCELLED);
        complaint.setResolvedAt(LocalDateTime.now());
        complaint.setResolvedBy(staff);
        if (staffNote != null && !staffNote.isBlank()) {
            complaint.setStaffNote(staffNote);
        }

        CustomerComplaint saved = complaintRepository.save(complaint);
        return buildComplaintResponseDto(saved);
    }

    /**
     * Tìm device thay thế (cùng model, available trong thời gian order còn lại)
     */
    private Device findReplacementDevice(Long deviceModelId, LocalDateTime start, LocalDateTime end) {
        List<Device> devices = deviceRepository.findByDeviceModel_DeviceModelId(deviceModelId);
        if (devices.isEmpty()) {
            return null;
        }

        // Lấy danh sách device đang busy trong khoảng thời gian
        Set<Long> busyDeviceIds = bookingCalendarRepository.findBusyDeviceIdsByModelAndRange(
                deviceModelId,
                start,
                end,
                EnumSet.of(BookingStatus.BOOKED, BookingStatus.ACTIVE)
        );

        // Tìm device available (không busy, status = AVAILABLE)
        return devices.stream()
                .filter(device -> device.getDeviceId() != null)
                .filter(device -> !busyDeviceIds.contains(device.getDeviceId()))
                .filter(device -> device.getStatus() == DeviceStatus.AVAILABLE)
                .findFirst()
                .orElse(null);
    }

    /**
     * Tự động đánh dấu task "Device Replacement" là hoàn thành khi resolve complaint
     * (Đảm bảo task được complete nếu chưa complete từ lúc customer ký biên bản)
     */
    private void markDeviceReplacementTaskCompleted(CustomerComplaint complaint) {
        if (complaint == null || complaint.getRentalOrder() == null) {
            return;
        }

        RentalOrder order = complaint.getRentalOrder();
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
            return; // Không có task pending, có thể đã complete từ lúc customer ký
        }

        // Đánh dấu tất cả task "Device Replacement" pending là completed
        LocalDateTime now = LocalDateTime.now();
        for (Task task : deliveryTasks) {
            task.setStatus(TaskStatus.COMPLETED);
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(now);
            }
            taskRepository.save(task);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerComplaintResponseDto getComplaintByTaskId(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("TaskId không được null");
        }

        com.rentaltech.techrental.staff.model.Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy task: " + taskId));

        String categoryName = task.getTaskCategory() != null ? task.getTaskCategory().getName() : null;
        Long orderId = task.getOrderId();

        List<CustomerComplaint> complaints;

        if ("Pre rental QC Replace".equalsIgnoreCase(categoryName)) {
            // Task "Pre rental QC Replace" → tìm complaint trực tiếp
            complaints = complaintRepository.findByReplacementTask_TaskId(taskId);
        } else if ("Device Replacement".equalsIgnoreCase(categoryName) && orderId != null) {
            // Task "Device Replacement" → tìm complaint theo orderId
            // Filter complaints có replacementReport hoặc replacementAllocation (đã được xử lý)
            complaints = complaintRepository.findByRentalOrder_OrderId(orderId).stream()
                    .filter(c -> c.getReplacementReport() != null || c.getReplacementAllocation() != null)
                    .filter(c -> c.getStatus() == ComplaintStatus.PROCESSING || c.getStatus() == ComplaintStatus.RESOLVED)
                    .collect(Collectors.toList());
        } else {
            // Fallback: thử tìm bằng replacementTask
            complaints = complaintRepository.findByReplacementTask_TaskId(taskId);
        }

        if (complaints == null || complaints.isEmpty()) {
            return null;
        }

        // Trả về complaint đầu tiên (hoặc có thể merge nếu có nhiều)
        return buildComplaintResponseDto(complaints.get(0));
    }

}

