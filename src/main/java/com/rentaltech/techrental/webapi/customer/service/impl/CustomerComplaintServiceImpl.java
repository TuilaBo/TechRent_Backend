package com.rentaltech.techrental.webapi.customer.service.impl;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.rentalorder.service.BookingCalendarService;
import com.rentaltech.techrental.rentalorder.repository.BookingCalendarRepository;
import com.rentaltech.techrental.rentalorder.model.BookingStatus;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.staff.service.taskservice.TaskService;
import com.rentaltech.techrental.webapi.customer.model.ComplaintStatus;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import org.springframework.web.multipart.MultipartFile;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.CustomerComplaint;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerComplaintRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerComplaintResponseDto;
import com.rentaltech.techrental.webapi.customer.repository.CustomerComplaintCustomRepository;
import com.rentaltech.techrental.webapi.customer.repository.CustomerComplaintRepository;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.customer.service.CustomerComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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

    @Override
    public CustomerComplaintResponseDto createComplaint(CustomerComplaintRequestDto request, String username) {
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
        return CustomerComplaintResponseDto.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerComplaintResponseDto> getMyComplaints(String username) {
        Customer customer = customerRepository.findByAccount_Username(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng"));

        List<CustomerComplaint> complaints = complaintCustomRepository.findByCustomerId(customer.getCustomerId());
        return complaints.stream()
                .map(CustomerComplaintResponseDto::from)
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

        return CustomerComplaintResponseDto.from(complaint);
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
                .map(CustomerComplaintResponseDto::from)
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
        return CustomerComplaintResponseDto.from(complaint);
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

        // Tìm device thay thế (cùng model, available trong toàn bộ khoảng thời gian của order)
        // Cần check từ startDate đến endDate để đảm bảo device không bị booking bởi order khác
        Device replacementDevice = findReplacementDevice(
                orderDetail.getDeviceModel().getDeviceModelId(),
                order.getEffectiveStartDate(),
                order.getEffectiveEndDate()
        );

        if (replacementDevice == null) {
            throw new IllegalStateException("Không tìm thấy thiết bị thay thế khả dụng. Vui lòng liên hệ bộ phận hỗ trợ.");
        }

        // Tạo Allocation mới cho device thay thế
        Allocation newAllocation = Allocation.builder()
                .device(replacementDevice)
                .orderDetail(orderDetail)
                .status("ALLOCATED")
                .allocatedAt(LocalDateTime.now())
                .notes("Thay thế cho device " + brokenDevice.getSerialNumber() + " (complaint #" + complaintId + ")")
                .build();
        Allocation savedAllocation = allocationRepository.save(newAllocation);

        // Update device cũ: mark as DAMAGED hoặc UNDER_MAINTENANCE
        brokenDevice.setStatus(DeviceStatus.DAMAGED);
        deviceRepository.save(brokenDevice);

        // Update device mới: mark as RESERVED hoặc PRE_RENTAL_QC
        replacementDevice.setStatus(DeviceStatus.RESERVED);
        deviceRepository.save(replacementDevice);

        // Tạo BookingCalendar cho device mới
        bookingCalendarService.createBookingsForAllocations(List.of(savedAllocation));

        // Tìm hoặc tạo TaskCategory "Device Replacement"
        TaskCategory replacementCategory = taskCategoryRepository.findByNameIgnoreCase("Device Replacement")
                .orElseGet(() -> {
                    // Nếu không có "Device Replacement", thử tìm "Delivery"
                    return taskCategoryRepository.findByNameIgnoreCase("Delivery")
                            .orElseGet(() -> {
                                // Nếu không có cả 2, tự động tạo category "Device Replacement"
                                TaskCategory newCategory = TaskCategory.builder()
                                        .name("Device Replacement")
                                        .description("Thay thế thiết bị cho khách hàng khi có khiếu nại")
                                        .build();
                                return taskCategoryRepository.save(newCategory);
                            });
                });

        // Tìm task pending của order (cùng category "Device Replacement")
        // Chỉ tạo 1 task cho tất cả complaints cùng order
        List<TaskStatus> pendingStatuses = List.of(
                com.rentaltech.techrental.staff.model.TaskStatus.PENDING,
                com.rentaltech.techrental.staff.model.TaskStatus.IN_PROGRESS
        );
        List<Task> existingTasks = taskCustomRepository.findPendingTasksByOrderAndCategory(
                order.getOrderId(),
                replacementCategory.getTaskCategoryId(),
                pendingStatuses
        );

        Task replacementTask;
        if (!existingTasks.isEmpty()) {
            // Đã có task pending → dùng task đó
            replacementTask = existingTasks.get(0); // Lấy task đầu tiên
            // Update description để bao gồm device mới
            updateTaskDescriptionWithNewDevice(replacementTask, brokenDevice, replacementDevice, complaintId);
        } else {
            // Chưa có task → tạo mới
            // Assign cho tất cả OPERATOR active; ai nhận trước sẽ assign tiếp cho technician
            List<Staff> operators = staffService.getStaffByRole(
                    com.rentaltech.techrental.staff.model.StaffRole.OPERATOR
            ).stream()
                    .filter(s -> s.getIsActive() != null && s.getIsActive())
                    .collect(Collectors.toList());

            List<Long> assignedOperatorIds = operators.isEmpty()
                    ? List.of(staff.getStaffId()) // fallback: assign người đang xử lý
                    : operators.stream().map(Staff::getStaffId).collect(Collectors.toList());

            TaskCreateRequestDto taskRequest = TaskCreateRequestDto.builder()
                    .taskCategoryId(replacementCategory.getTaskCategoryId())
                    .orderId(order.getOrderId())
                    .assignedStaffIds(assignedOperatorIds)
                    .description(String.format("Thay thế thiết bị cho đơn hàng #%d. Khiếu nại #%d: %s (Serial: %s) → %s (Serial: %s). Vui lòng assign staff đi giao máy.",
                            order.getOrderId(),
                            complaintId,
                            brokenDevice.getDeviceModel().getDeviceName(),
                            brokenDevice.getSerialNumber(),
                            replacementDevice.getDeviceModel().getDeviceName(),
                            replacementDevice.getSerialNumber()))
                    .plannedStart(LocalDateTime.now())
                    .plannedEnd(LocalDateTime.now().plusHours(24)) // Deadline 24h
                    .build();

            replacementTask = taskService.createTask(taskRequest, username);
        }

        // Update complaint
        complaint.setStatus(ComplaintStatus.PROCESSING);
        complaint.setFaultSource(faultSource != null
                ? faultSource
                : com.rentaltech.techrental.webapi.customer.model.ComplaintFaultSource.UNKNOWN);
        complaint.setStaffNote(staffNote);
        complaint.setReplacementDevice(replacementDevice);
        complaint.setReplacementTask(replacementTask);
        complaint.setReplacementAllocation(savedAllocation);
        complaint.setProcessedAt(LocalDateTime.now());

        // Ghi nhận condition hiện tại của thiết bị để cập nhật trạng thái
        if (conditionDefinitionIds != null && !conditionDefinitionIds.isEmpty()) {
            addDamageConditions(brokenDevice.getDeviceId(), conditionDefinitionIds, damageNote, staff.getStaffId());
        }

        CustomerComplaint saved = complaintRepository.save(complaint);
        
        // OPTION 3: Tự động tạo biên bản (hoặc thêm vào biên bản pending nếu có)
        // Biên bản sẽ được tạo ngay khi process, tự động gộp nếu đã có biên bản pending của order
        deviceReplacementReportService.createDeviceReplacementReport(saved.getComplaintId(), username);
        
        // Reload để lấy replacement report
        saved = complaintRepository.findById(saved.getComplaintId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khiếu nại sau khi tạo biên bản"));
        
        return CustomerComplaintResponseDto.from(saved);
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
        return CustomerComplaintResponseDto.from(saved);
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

        // Tự động update task status thành COMPLETED
        if (replacementTask != null && replacementTask.getTaskId() != null) {
            replacementTask.setStatus(TaskStatus.COMPLETED);
            replacementTask.setCompletedAt(LocalDateTime.now());
            taskRepository.save(replacementTask);
        }

        return CustomerComplaintResponseDto.from(saved);
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
        return CustomerComplaintResponseDto.from(saved);
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

        // Tìm device available (không busy, không damaged/lost, status = AVAILABLE hoặc RESERVED)
        return devices.stream()
                .filter(device -> device.getDeviceId() != null)
                .filter(device -> !busyDeviceIds.contains(device.getDeviceId()))
                .filter(device -> device.getStatus() == DeviceStatus.AVAILABLE || device.getStatus() == DeviceStatus.RESERVED)
                .filter(device -> device.getStatus() != DeviceStatus.DAMAGED && device.getStatus() != DeviceStatus.LOST)
                .findFirst()
                .orElse(null);
    }

}

