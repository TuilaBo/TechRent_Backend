package com.rentaltech.techrental.webapi.technician.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.device.model.*;
import com.rentaltech.techrental.device.model.dto.DeviceConditionResponseDto;
import com.rentaltech.techrental.device.model.dto.DiscrepancyInlineRequestDto;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportRequestDto;
import com.rentaltech.techrental.device.repository.*;
import com.rentaltech.techrental.device.service.AllocationSnapshotService;
import com.rentaltech.techrental.device.service.DeviceConditionService;
import com.rentaltech.techrental.device.service.DiscrepancyReportService;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.OrderDetailRepository;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.rentalorder.service.ReservationService;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.staff.service.taskservice.TaskService;
import com.rentaltech.techrental.staff.service.devicereplacement.DeviceReplacementReportService;
import com.rentaltech.techrental.webapi.customer.model.CustomerComplaint;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.repository.CustomerComplaintRepository;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import com.rentaltech.techrental.webapi.technician.model.QCPhase;
import com.rentaltech.techrental.webapi.technician.model.QCReport;
import com.rentaltech.techrental.webapi.technician.model.QCResult;
import com.rentaltech.techrental.webapi.technician.model.dto.*;
import com.rentaltech.techrental.webapi.technician.repository.QCReportRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QCReportServiceImpl implements QCReportService {

    private static final Logger log = LoggerFactory.getLogger(QCReportServiceImpl.class);
    private static final String STAFF_NOTIFICATION_TOPIC_TEMPLATE = "/topic/staffs/%d/notifications";

    private final QCReportRepository qcReportRepository;
    private final TaskRepository taskRepository;
    private final DeviceRepository deviceRepository;
    private final AllocationConditionSnapshotRepository allocationConditionSnapshotRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final AllocationRepository allocationRepository;
    private final ConditionDefinitionRepository conditionDefinitionRepository;
    private final AllocationSnapshotService allocationSnapshotService;
    private final DeviceConditionService deviceConditionService;
    private final AccountService accountService;
    private final StaffService staffService;
    private final NotificationService notificationService;
    private final ImageStorageService imageStorageService;
    private final com.rentaltech.techrental.rentalorder.service.BookingCalendarService bookingCalendarService;
    private final ReservationService reservationService;
    private final RentalOrderRepository rentalOrderRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DiscrepancyReportService discrepancyReportService;
    private final DiscrepancyReportRepository discrepancyReportRepository;
    private final CustomerComplaintRepository customerComplaintRepository;
    private final DeviceReplacementReportService deviceReplacementReportService;
    private final TaskService taskService;
    private final TaskCategoryRepository taskCategoryRepository;

    @Override
    @Transactional
    public QCReportResponseDto createPreRentalReport(QCReportPreRentalCreateRequestDto request, MultipartFile accessorySnapshot, String username) {
        return createReport(QCPhase.PRE_RENTAL, request.getTaskId(), request.getOrderDetailSerialNumbers(),
                request.getResult(), request.getFindings(), List.of(), request.getDeviceConditions(), accessorySnapshot, username);
    }

    @Override
    @Transactional
    public QCReportResponseDto createPostRentalReport(QCReportPostRentalCreateRequestDto request, MultipartFile accessorySnapshot, String username) {
        return createReport(QCPhase.POST_RENTAL, request.getTaskId(), request.getOrderDetailSerialNumbers(),
                request.getResult(), request.getFindings(), request.getDiscrepancies(), List.of(), accessorySnapshot, username);
    }

    private QCReportResponseDto createReport(QCPhase phase,
                                             Long taskId,
                                             Map<Long, List<String>> orderDetailSerialNumbers,
                                             QCResult result,
                                             String findings,
                                             List<DiscrepancyInlineRequestDto> discrepancies,
                                             List<QCDeviceConditionRequestDto> deviceConditions,
                                             MultipartFile accessorySnapshot,
                                             String username) {
        Account account = getAccountByUsername(username);
        Task task = getTaskById(taskId);
        Staff currentStaff = staffService.getStaffByAccountIdOrThrow(account.getAccountId());

        if (!isStaffAssignedToTask(task, currentStaff)) {
            throw new AccessDeniedException("Kỹ thuật viên không được phân công cho công việc này");
        }

        qcReportRepository.findByTask_TaskId(task.getTaskId())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Đã tồn tại báo cáo QC cho công việc này");
                });

        validatePhaseAndResult(phase, result);
        Map<OrderDetail, List<String>> orderDetailSerials = resolveOrderDetails(task, orderDetailSerialNumbers);
        RentalOrder rentalOrder = determineRentalOrder(task, orderDetailSerials);
        if (phase == QCPhase.PRE_RENTAL) {
            ensureRentalOrderProcessing(rentalOrder, task);
        }

        QCReport qcReport = QCReport.builder()
                .phase(phase)
                .result(result)
                .findings(findings)
                .createdBy(username)
                .task(task)
                .rentalOrder(rentalOrder)
                .build();

        maybeUploadAccessorySnapshot(accessorySnapshot, qcReport);
        QCReport saved = qcReportRepository.save(qcReport);
        qcReportRepository.flush();
        if (phase == QCPhase.POST_RENTAL) {
            createPostRentalSnapshots(orderDetailSerials, currentStaff);
        }

        List<Allocation> allocations = new ArrayList<>();
        
        // Chỉ tạo allocation khi QC PASS (READY_FOR_SHIPPING)
        if (result == QCResult.READY_FOR_SHIPPING) {
            allocations = createAllocationsIfNeeded(saved, orderDetailSerials);
            if (!allocations.isEmpty()) {
                saved.setAllocations(new ArrayList<>(allocations));
                bookingCalendarService.createBookingsForAllocations(allocations);
                extendReservationHold(task, allocations, saved.getResult());
                
                // Nếu là PRE_RENTAL và có allocation → update complaint nếu task này là replacement task
                if (phase == QCPhase.PRE_RENTAL) {
                    updateComplaintAfterQcAllocation(task, allocations);
                    createBaselineSnapshots(allocations, deviceConditions, currentStaff);
                }
            }
        } else if (phase == QCPhase.PRE_RENTAL && result == QCResult.PRE_RENTAL_FAILED) {
            // Xử lý khi QC FAILED: reset device status và complaint
            handlePreRentalQcFailed(task, orderDetailSerials);
        }
        if (!CollectionUtils.isEmpty(discrepancies)) {
            handleDiscrepancies(discrepancies, DiscrepancyCreatedFrom.QC_REPORT, saved.getQcReportId(), currentStaff);
        }
        if (phase == QCPhase.POST_RENTAL && saved.getResult() == QCResult.READY_FOR_RE_STOCK) {
            updateDevicesAfterPostRentalQc(saved);
        }
        markTaskCompleted(task);
        notifyCustomer(saved);
        QcReportDtoContext context = prepareQcReportDtoContext(saved);
        return QCReportResponseDto.from(saved, context.allocations(), context.discrepancies());
    }

    @Override
    @Transactional(readOnly = true)
    public QCReportResponseDto getReport(Long reportId) {
        QCReport report = qcReportRepository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy báo cáo QC với id: " + reportId));
        QcReportDtoContext context = prepareQcReportDtoContext(report);
        return QCReportResponseDto.from(report, context.allocations(), context.discrepancies());
    }

    @Override
    @Transactional
    public QCReportResponseDto updatePreRentalReport(Long reportId,
                                                     QCReportPreRentalUpdateRequestDto request,
                                                     MultipartFile accessorySnapshot,
                                                     String username) {
        return updateReportInternal(
                reportId,
                QCPhase.PRE_RENTAL,
                request.getResult(),
                request.getFindings(),
                request.getOrderDetailSerialNumbers(),
                request.getDeviceConditions(),
                List.of(),
                accessorySnapshot,
                username
        );
    }

    @Override
    @Transactional
    public QCReportResponseDto updatePostRentalReport(Long reportId,
                                                      QCReportPostRentalUpdateRequestDto request,
                                                      MultipartFile accessorySnapshot,
                                                      String username) {
        return updateReportInternal(
                reportId,
                QCPhase.POST_RENTAL,
                request.getResult(),
                request.getFindings(),
                request.getOrderDetailSerialNumbers(),
                List.of(),
                request.getDiscrepancies(),
                accessorySnapshot,
                username
        );
    }

    private QCReportResponseDto updateReportInternal(Long reportId,
                                                     QCPhase phase,
                                                     QCResult result,
                                                     String findings,
                                                     Map<Long, List<String>> serialNumbersByOrderDetail,
                                                     List<QCDeviceConditionRequestDto> deviceConditions,
                                                     List<DiscrepancyInlineRequestDto> discrepancies,
                                                     MultipartFile accessorySnapshot,
                                                     String username) {
        QCReport report = qcReportRepository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy báo cáo QC với id: " + reportId));

        Account account = getAccountByUsername(username);
        boolean isAdmin = account.getRole() == Role.ADMIN;
        Staff currentStaff = null;

        if (!isAdmin) {
            currentStaff = staffService.getStaffByAccountIdOrThrow(account.getAccountId());
            if (!isStaffAssignedToTask(report.getTask(), currentStaff)) {
                throw new AccessDeniedException("Kỹ thuật viên không được phân công cho công việc này");
            }
        }

        if (report.getPhase() != phase) {
            throw new IllegalArgumentException("Báo cáo QC thuộc giai đoạn " + report.getPhase() + " không thể cập nhật qua endpoint " + phase);
        }

        RentalOrder rentalOrder = resolveRentalOrderForReport(report);
        if (phase == QCPhase.PRE_RENTAL) {
            ensureRentalOrderProcessing(rentalOrder, report.getTask());
        }
        report.setRentalOrder(rentalOrder);

        validatePhaseAndResult(phase, result);

        // Validation: Kiểm tra xem task có phải là replacement task cho complaint không
        if (report.getTask() != null) {
            validateTaskForComplaint(report.getTask());
        }

        report.setPhase(phase);
        report.setResult(result);
        report.setFindings(findings);
        maybeUploadAccessorySnapshot(accessorySnapshot, report);

        if (phase == QCPhase.PRE_RENTAL) {
            handlePreRentalUpdate(report, result, serialNumbersByOrderDetail, deviceConditions, currentStaff);
        } else if (phase == QCPhase.POST_RENTAL) {
            handlePostRentalUpdate(report, result);
        } else {
            throw new UnsupportedOperationException("Chưa hỗ trợ cập nhật cho giai đoạn QC: " + phase);
        }

        QCReport updated = qcReportRepository.save(report);
        qcReportRepository.flush();
        handleDiscrepancies(discrepancies, DiscrepancyCreatedFrom.QC_REPORT, updated.getQcReportId(), currentStaff);
        notifyCustomer(updated);
        QcReportDtoContext context = prepareQcReportDtoContext(updated);
        return QCReportResponseDto.from(updated, context.allocations(), context.discrepancies());
    }

    private void handlePreRentalUpdate(QCReport report,
                                       QCResult result,
                                       Map<Long, List<String>> serialNumbersByOrderDetail,
                                       List<QCDeviceConditionRequestDto> deviceConditions,
                                       Staff currentStaff) {
        if (result != QCResult.READY_FOR_SHIPPING) {
            // TODO: handle PRE_RENTAL updates for other results
            return;
        }
        List<Allocation> finalAllocations;
        if (serialNumbersByOrderDetail != null) {
            Map<OrderDetail, List<String>> resolved = resolveOrderDetails(report.getTask(), serialNumbersByOrderDetail);
            if (resolved.isEmpty()) {
                throw new IllegalArgumentException("Cần cung cấp danh sách thiết bị để tạo allocation");
            }
            RentalOrder rentalOrder = determineRentalOrder(report.getTask(), resolved);
            ensureRentalOrderProcessing(rentalOrder, report.getTask());
            report.setRentalOrder(rentalOrder);
            if (report.getTask() != null && report.getTask().getOrderId() != null) {
                bookingCalendarService.clearBookingsForOrder(report.getTask().getOrderId());
            }
            clearExistingAllocations(report);
            List<Allocation> allocations = createAllocations(report, resolved);
            report.setAllocations(new ArrayList<>(allocations));
            bookingCalendarService.createBookingsForAllocations(allocations);
            finalAllocations = allocations;
        } else if (report.getAllocations() == null || report.getAllocations().isEmpty()) {
            throw new IllegalArgumentException("Cần cung cấp danh sách thiết bị khi kết quả READY_FOR_SHIPPING");
        } else {
            finalAllocations = report.getAllocations();
        }
        createBaselineSnapshots(finalAllocations, deviceConditions, currentStaff);
        extendReservationHold(report.getTask(), finalAllocations, report.getResult());
        
        // Update complaint nếu task này là replacement task
        updateComplaintAfterQcAllocation(report.getTask(), finalAllocations);
    }

    private void handlePostRentalUpdate(QCReport report, QCResult result) {
        if (result == QCResult.READY_FOR_RE_STOCK) {
            updateDevicesAfterPostRentalQc(report);
        }
        // TODO: handle POST_RENTAL updates for other results
    }

    private void handleDiscrepancies(List<DiscrepancyInlineRequestDto> discrepancies,
                                     DiscrepancyCreatedFrom createdFrom,
                                     Long refId,
                                     Staff staff) {
        if (refId == null || discrepancies == null || discrepancies.isEmpty()) {
            return;
        }
        Long staffId = staff != null ? staff.getStaffId() : null;
        discrepancies.stream()
                .map(DiscrepancyInlineRequestDto::getDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .forEach(deviceConditionService::deleteByDevice);
        discrepancies.stream()
                .filter(Objects::nonNull)
                .forEach(dto -> discrepancyReportService.create(
                        DiscrepancyReportRequestDto.builder()
                                .createdFrom(createdFrom)
                                .refId(refId)
                                .discrepancyType(dto.getDiscrepancyType())
                                .conditionDefinitionId(dto.getConditionDefinitionId())
                                .orderDetailId(dto.getOrderDetailId())
                                .deviceId(dto.getDeviceId())
                                .staffNote(dto.getStaffNote())
                                .build()
                ));
        discrepancies.stream()
                .filter(dto -> dto != null && dto.getDeviceId() != null && dto.getConditionDefinitionId() != null)
                .forEach(dto -> deviceConditionService.updateCondition(
                        dto.getDeviceId(),
                        dto.getConditionDefinitionId(),
                        dto.getDiscrepancyType() != null ? dto.getDiscrepancyType().name() : null,
                        dto.getStaffNote(),
                        null,
                        staffId));
    }

    private void notifyCustomer(QCReport qcReport) {
        switch (qcReport.getPhase()) {
            case PRE_RENTAL -> {
                if (qcReport.getResult() == QCResult.READY_FOR_SHIPPING) {
                    notificationService.notifyAccount(
                            qcReport.getRentalOrder().getCustomer().getAccount().getAccountId(),
                            NotificationType.ORDER_CONFIRMED,
                            "Đơn hàng đã được xác nhận",
                            "Đơn hàng #" + qcReport.getTask().getOrderId() + " đã vượt qua kiểm tra và sẵn sàng giao."
                    );
                } else if (qcReport.getResult() == QCResult.PRE_RENTAL_FAILED) {
                    notificationService.notifyAccount(
                            qcReport.getRentalOrder().getCustomer().getAccount().getAccountId(),
                            NotificationType.ORDER_ISSUE,
                            "Vấn đề với đơn hàng của bạn",
                            "Đơn hàng #" + qcReport.getTask().getOrderId() + " không vượt qua kiểm tra chất lượng. Vui lòng liên hệ bộ phận hỗ trợ."
                    );
                }
            }
            case POST_RENTAL -> {
                if (qcReport.getResult() == QCResult.READY_FOR_RE_STOCK) {
                    notificationService.notifyAccount(
                            qcReport.getRentalOrder().getCustomer().getAccount().getAccountId(),
                            NotificationType.ORDER_CONFIRMED,
                            "Đơn thuê đã được hoàn trả",
                            "Đơn thuê #" + qcReport.getTask().getOrderId() + " đã được kiểm tra sau thuê và hoàn trả thành công."
                    );
                } else if (qcReport.getResult() == QCResult.POST_RENTAL_FAILED) {
                    notificationService.notifyAccount(
                            qcReport.getRentalOrder().getCustomer().getAccount().getAccountId(),
                            NotificationType.ORDER_ISSUE,
                            "Vấn đề với đơn thuê của bạn",
                            "Đơn thuê #" + qcReport.getTask().getOrderId() + " không vượt qua kiểm tra chất lượng sau thuê. Vui lòng liên hệ bộ phận hỗ trợ."
                    );
                }
            }
        }
        notifySupportStaffPostRental(qcReport.getTask());
    }

    private void createPostRentalSnapshots(Map<OrderDetail, List<String>> orderDetailSerials, Staff staff) {
        List<Allocation> allocations = resolveAllocationsForOrderDetails(orderDetailSerials);
        allocationSnapshotService.createSnapshots(
                allocations,
                AllocationSnapshotType.FINAL,
                AllocationSnapshotSource.QC_AFTER,
                staff);
    }

    private List<Allocation> resolveAllocationsForOrderDetails(Map<OrderDetail, List<String>> orderDetailSerials) {
        if (orderDetailSerials == null || orderDetailSerials.isEmpty()) {
            return List.of();
        }
        Set<Allocation> allocationSet = new LinkedHashSet<>();
        orderDetailSerials.forEach((detail, serials) -> {
            if (detail == null || CollectionUtils.isEmpty(serials)) {
                return;
            }
            serials.stream()
                    .filter(serial -> serial != null && !serial.isBlank())
                    .forEach(serial -> {
                        Allocation allocation = allocationRepository
                                .findByOrderDetail_OrderDetailIdAndDevice_SerialNumber(detail.getOrderDetailId(), serial)
                                .orElseThrow(() -> new NoSuchElementException(
                                        "Không tìm thấy allocation cho order detail " + detail.getOrderDetailId()
                                                + " và serial " + serial));
                        allocationSet.add(allocation);
                    });
        });
        return new ArrayList<>(allocationSet);
    }

    private void notifySupportStaffPostRental(Task task) {
        if (task == null || task.getAssignedStaff() == null || task.getAssignedStaff().isEmpty()) {
            return;
        }
        Long orderId = task.getOrderId();
        PostRentalQCNotification payload = new PostRentalQCNotification(
                task.getTaskId(),
                orderId,
                "QC sau thuê cho đơn #" + (orderId != null ? orderId : "") + " đã hoàn tất. Vui lòng hỗ trợ khách theo quy trình trả hàng."
        );
        task.getAssignedStaff().stream()
                .filter(Objects::nonNull)
                .filter(staff -> staff.getStaffRole() == StaffRole.CUSTOMER_SUPPORT_STAFF)
                .forEach(staff -> {
                    // Persist notification for staff account
                    if (staff.getAccount() != null && staff.getAccount().getAccountId() != null) {
                        notificationService.notifyAccount(
                                staff.getAccount().getAccountId(),
                                NotificationType.ORDER_NEAR_DUE,
                                "Hoàn tất QC sau thuê",
                                payload.message()
                        );
                    }
                    Long staffId = staff.getStaffId();
                    if (staffId == null) {
                        return;
                    }
                    String destination = String.format(STAFF_NOTIFICATION_TOPIC_TEMPLATE, staffId);
                    try {
                        messagingTemplate.convertAndSend(destination, payload);
                    } catch (Exception ex) {
                        log.warn("Không thể gửi thông báo QC sau thuê cho nhân viên hỗ trợ {}: {}", staffId, ex.getMessage());
                    }
                });
    }

    private record PostRentalQCNotification(Long taskId, Long orderId, String message) {}
    private record QcReportDtoContext(List<Allocation> allocations, List<DiscrepancyReport> discrepancies) {}

    @Override
    @Transactional(readOnly = true)
    public List<QCReportResponseDto> getReportsByOrder(Long rentalOrderId) {
        List<Task> qcTasks = taskRepository.findByOrderId(rentalOrderId).stream()
                .filter(task -> {
                    String type = task.getTaskCategory().getName();
                    return type != null && (type.equalsIgnoreCase("Pre rental QC") 
                            || type.equalsIgnoreCase("Pre rental QC Replace")
                            || type.equalsIgnoreCase("Post rental QC"));
                })
                .toList();

        if (qcTasks.isEmpty()) {
            return List.of();
        }

        List<Long> qcTaskIds = qcTasks.stream()
                .map(Task::getTaskId)
                .collect(Collectors.toList());

        if (qcTaskIds.isEmpty()) {
            return List.of();
        }

        return qcReportRepository.findByTask_TaskIdIn(qcTaskIds).stream()
                .map(report -> {
                    QcReportDtoContext context = prepareQcReportDtoContext(report);
                    return QCReportResponseDto.from(report, context.allocations(), context.discrepancies());
                })
                .collect(Collectors.toList());
    }

    private void updateDevicesAfterPostRentalQc(QCReport report) {
        List<Allocation> resolvedAllocations = resolveAllocations(report);
        Long orderId = resolveOrderId(report, resolvedAllocations);
        if (orderId == null) {
            return;
        }
        List<DiscrepancyReport> orderDiscrepancies = discrepancyReportRepository
                .findByAllocation_OrderDetail_RentalOrder_OrderId(orderId);
        Map<Long, List<DiscrepancyReport>> discrepanciesByDeviceId = orderDiscrepancies.stream()
                .filter(discrepancy -> discrepancy.getAllocation() != null
                        && discrepancy.getAllocation().getDevice() != null
                        && discrepancy.getAllocation().getDevice().getDeviceId() != null)
                .collect(Collectors.groupingBy(discrepancy -> discrepancy.getAllocation().getDevice().getDeviceId()));
        if (CollectionUtils.isEmpty(resolvedAllocations)) {
            return;
        }
        List<Device> devicesToUpdate = new ArrayList<>();
        for (Allocation allocation : resolvedAllocations) {
            if (allocation == null || allocation.getDevice() == null || allocation.getDevice().getDeviceId() == null) {
                continue;
            }
            Device device = allocation.getDevice();
            List<DiscrepancyReport> deviceDiscrepancies = discrepanciesByDeviceId.get(device.getDeviceId());
            if (CollectionUtils.isEmpty(deviceDiscrepancies)) {
                device.setStatus(DeviceStatus.AVAILABLE);
                devicesToUpdate.add(device);
                continue;
            }
            boolean hasMissing = deviceDiscrepancies.stream()
                    .anyMatch(discrepancy -> discrepancy.getDiscrepancyType() == DiscrepancyType.MISSING_ITEM);
            if (hasMissing) {
                device.setStatus(DeviceStatus.LOST);
                devicesToUpdate.add(device);
                continue;
            }
            boolean hasDamage = deviceDiscrepancies.stream()
                    .anyMatch(discrepancy -> discrepancy.getDiscrepancyType() == DiscrepancyType.DAMAGE);
            if (hasDamage) {
                device.setStatus(DeviceStatus.DAMAGED);
                devicesToUpdate.add(device);
            }
        }
        if (devicesToUpdate.isEmpty()) {
            return;
        }
        deviceRepository.saveAll(devicesToUpdate);
        deviceRepository.flush();
    }

    private void createBaselineSnapshots(List<Allocation> allocations,
                                         List<QCDeviceConditionRequestDto> deviceConditions,
                                         Staff staff) {
        if (CollectionUtils.isEmpty(allocations)) {
            return;
        }
        Map<Long, Allocation> allocationsByDeviceId = allocations.stream()
                .filter(Objects::nonNull)
                .filter(allocation -> allocation.getDevice() != null && allocation.getDevice().getDeviceId() != null)
                .collect(Collectors.toMap(
                        allocation -> allocation.getDevice().getDeviceId(),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        if (allocationsByDeviceId.isEmpty()) {
            allocationSnapshotService.createSnapshots(
                    allocations,
                    AllocationSnapshotType.BASELINE,
                    AllocationSnapshotSource.QC_BEFORE,
                    staff);
            return;
        }
        List<QCDeviceConditionRequestDto> normalizedConditions = deviceConditions == null ? List.of() : deviceConditions;
        Map<Long, List<QCDeviceConditionRequestDto>> conditionsByDeviceId = normalizedConditions.stream()
                .filter(Objects::nonNull)
                .filter(condition -> condition.getDeviceId() != null && condition.getConditionDefinitionId() != null)
                .collect(Collectors.groupingBy(
                        QCDeviceConditionRequestDto::getDeviceId,
                        LinkedHashMap::new,
                        Collectors.toList()));
        conditionsByDeviceId.keySet().stream()
                .filter(Objects::nonNull)
                .forEach(deviceConditionService::deleteByDevice);
        Set<Long> conditionDefinitionIds = normalizedConditions.stream()
                .filter(Objects::nonNull)
                .map(QCDeviceConditionRequestDto::getConditionDefinitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ConditionDefinition> conditionDefinitionsById = conditionDefinitionIds.isEmpty()
                ? Collections.emptyMap()
                : conditionDefinitionRepository.findAllById(conditionDefinitionIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ConditionDefinition::getConditionDefinitionId, Function.identity()));
        Map<Long, List<ConditionSnapshotPayload>> payloadsByDevice = new LinkedHashMap<>();
        conditionsByDeviceId.forEach((deviceId, conditions) -> {
            List<ConditionSnapshotPayload> payloads = conditions.stream()
                    .map(condition -> {
                        ConditionDefinition definition = conditionDefinitionsById.get(condition.getConditionDefinitionId());
                        List<String> images = CollectionUtils.isEmpty(condition.getImages())
                                ? new ArrayList<>()
                                : new ArrayList<>(condition.getImages());
                        return new ConditionSnapshotPayload(
                                condition.getConditionDefinitionId(),
                                definition != null ? definition.getName() : null,
                                condition.getSeverity(),
                                images,
                                true);
                    })
                    .toList();
            payloadsByDevice.put(deviceId, new ArrayList<>(payloads));
        });
        allocationsByDeviceId.keySet().forEach(deviceId -> {
            if (!payloadsByDevice.containsKey(deviceId)) {
                List<DeviceConditionResponseDto> stored = deviceConditionService.getByDevice(deviceId);
                if (!CollectionUtils.isEmpty(stored)) {
                            payloadsByDevice.put(
                                    deviceId,
                                    stored.stream()
                                            .map(dc -> new ConditionSnapshotPayload(
                                                    dc.getConditionDefinitionId(),
                                                    dc.getConditionDefinitionName(),
                                                    dc.getSeverity(),
                                                    dc.getImages() == null ? new ArrayList<>() : new ArrayList<>(dc.getImages()),
                                                    false))
                                            .toList());
                }
            }
        });
        if (payloadsByDevice.isEmpty()) {
            allocationSnapshotService.createSnapshots(
                    allocations,
                    AllocationSnapshotType.BASELINE,
                    AllocationSnapshotSource.QC_BEFORE,
                    staff);
            return;
        }
        List<AllocationConditionSnapshot> snapshots = new ArrayList<>();
        final Long staffId = staff != null ? staff.getStaffId() : null;
        payloadsByDevice.forEach((deviceId, payloads) -> {
            Allocation allocation = allocationsByDeviceId.get(deviceId);
            if (allocation == null) {
                log.warn("Không tìm thấy allocation tương ứng cho device {}", deviceId);
                return;
            }
            removeExistingQcBaselineSnapshots(allocation);
            for (ConditionSnapshotPayload payload : payloads) {
                if (payload.conditionDefinitionId() == null) {
                    continue;
                }
                AllocationConditionSnapshot snapshot = AllocationConditionSnapshot.builder()
                        .allocation(allocation)
                        .snapshotType(AllocationSnapshotType.BASELINE)
                        .source(AllocationSnapshotSource.QC_BEFORE)
                        .conditionDetails(List.of(AllocationConditionDetail.builder()
                                .conditionDefinitionId(payload.conditionDefinitionId())
                                .conditionDefinitionName(payload.conditionDefinitionName())
                                .severity(payload.severity())
                                .build()))
                        .images(new ArrayList<>(payload.images()))
                        .staff(staff)
                        .build();
                snapshots.add(snapshot);
                if (payload.shouldPersist()) {
                    deviceConditionService.updateCondition(
                            deviceId,
                            payload.conditionDefinitionId(),
                            payload.severity(),
                            null,
                            payload.images(),
                            staffId);
                }
            }
        });
        if (snapshots.isEmpty()) {
            allocationSnapshotService.createSnapshots(
                    allocations,
                    AllocationSnapshotType.BASELINE,
                    AllocationSnapshotSource.QC_BEFORE,
                    staff);
            return;
        }
        allocationConditionSnapshotRepository.saveAll(snapshots);
        allocationConditionSnapshotRepository.flush();
        Map<Allocation, List<AllocationConditionSnapshot>> snapshotsByAllocation = snapshots.stream()
                .filter(snapshot -> snapshot.getAllocation() != null)
                .collect(Collectors.groupingBy(AllocationConditionSnapshot::getAllocation));
        snapshotsByAllocation.forEach((allocation, allocationSnapshots) -> {
            if (allocation.getBaselineSnapshots() == null) {
                allocation.setBaselineSnapshots(new ArrayList<>());
            }
            allocation.getBaselineSnapshots().addAll(allocationSnapshots);
        });
    }

    private void removeExistingQcBaselineSnapshots(Allocation allocation) {
        if (allocation == null) {
            return;
        }
        Long allocationId = allocation.getAllocationId();
        if (allocationId != null) {
            allocationConditionSnapshotRepository.deleteByAllocation_AllocationIdAndSnapshotTypeAndSource(
                    allocationId,
                    AllocationSnapshotType.BASELINE,
                    AllocationSnapshotSource.QC_BEFORE);
        }
        if (allocation.getBaselineSnapshots() != null) {
            allocation.getBaselineSnapshots().removeIf(snapshot ->
                    snapshot != null
                            && snapshot.getSnapshotType() == AllocationSnapshotType.BASELINE
                            && snapshot.getSource() == AllocationSnapshotSource.QC_BEFORE);
        }
    }

    private void clearExistingAllocations(QCReport report) {
        if (report == null) {
            return;
        }
        List<Allocation> existing;
        if (report.getQcReportId() != null) {
            existing = allocationRepository.findByQcReport_QcReportId(report.getQcReportId());
        } else if (report.getAllocations() != null) {
            existing = new ArrayList<>(report.getAllocations());
        } else {
            existing = List.of();
        }
        if (existing.isEmpty()) {
            return;
        }
        existing.forEach(this::removeExistingQcBaselineSnapshots);
        allocationRepository.deleteAll(existing);
        allocationRepository.flush();
        if (report.getAllocations() != null) {
            report.getAllocations().clear();
        }
    }

    private List<Allocation> createAllocationsIfNeeded(QCReport report, Map<OrderDetail, List<String>> orderDetailSerials) {
        if (report.getPhase() == QCPhase.POST_RENTAL) {
            return resolveAllocations(report);
        }
        if (orderDetailSerials == null || orderDetailSerials.isEmpty()) {
            throw new IllegalArgumentException("Cần cung cấp danh sách thiết bị để tạo allocation");
        }
        return createAllocations(report, orderDetailSerials);
    }

    private void markTaskCompleted(Task task) {
        if (task == null) {
            return;
        }
        boolean requiresUpdate = task.getStatus() != TaskStatus.COMPLETED || task.getCompletedAt() == null;
        if (!requiresUpdate) {
            return;
        }
        task.setStatus(TaskStatus.COMPLETED);
        if (task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        }
        taskRepository.save(task);
        taskRepository.flush();
    }

    private List<Allocation> createAllocations(QCReport report, Map<OrderDetail, List<String>> orderDetailSerials) {
        List<Allocation> allocations = new ArrayList<>();
        Set<Device> devicesToUpdate = new LinkedHashSet<>();
        orderDetailSerials.forEach((orderDetail, serialNumbers) -> {
            if (serialNumbers == null || serialNumbers.isEmpty()) {
                throw new IllegalArgumentException("Cần cung cấp danh sách thiết bị để tạo allocation cho order detail "
                        + (orderDetail != null ? orderDetail.getOrderDetailId() : "không xác định"));
            }
            for (String serial : serialNumbers) {
                Device device = deviceRepository.findBySerialNumber(serial)
                        .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thiết bị với serial: " + serial));
                if (orderDetail != null && orderDetail.getDeviceModel() != null && device.getDeviceModel() != null) {
                    Long expectedModel = orderDetail.getDeviceModel().getDeviceModelId();
                    if (!expectedModel.equals(device.getDeviceModel().getDeviceModelId())) {
                        throw new IllegalArgumentException("Thiết bị " + serial + " không thuộc model của OrderDetail "
                                + orderDetail.getOrderDetailId());
                    }
                }
                device.setStatus(DeviceStatus.PRE_RENTAL_QC);
                devicesToUpdate.add(device);
                Allocation allocation = Allocation.builder()
                        .device(device)
                        .orderDetail(orderDetail)
                        .qcReport(report)
                        .status("ALLOCATED")
                        .allocatedAt(LocalDateTime.now())
                        .build();
                allocations.add(allocation);
            }
        });
        if (!devicesToUpdate.isEmpty()) {
            deviceRepository.saveAll(devicesToUpdate);
            deviceRepository.flush();
        }
        List<Allocation> persisted = allocationRepository.saveAll(allocations);
        allocationRepository.flush();
        return persisted;
    }

    private Map<OrderDetail, List<String>> resolveOrderDetails(Task task, Map<Long, List<String>> orderDetailSerialNumbers) {
        if (orderDetailSerialNumbers == null || orderDetailSerialNumbers.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<OrderDetail, List<String>> resolved = new LinkedHashMap<>();
        orderDetailSerialNumbers.forEach((orderDetailId, serialNumbers) -> {
            if (orderDetailId == null) {
                throw new IllegalArgumentException("orderDetailId không được để trống");
            }
            OrderDetail orderDetail = orderDetailRepository.findById(orderDetailId)
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy OrderDetail với id: " + orderDetailId));
            if (task != null) {
                Long taskOrderId = task.getOrderId();
                Long orderId = orderDetail.getRentalOrder() != null ? orderDetail.getRentalOrder().getOrderId() : null;
                if (taskOrderId != null && orderId != null && !Objects.equals(taskOrderId, orderId)) {
                    throw new IllegalArgumentException("Order detail " + orderDetailId + " không thuộc cùng đơn hàng với tác vụ");
                }
            }
            resolved.put(orderDetail, serialNumbers);
        });
        return resolved;
    }

    private void maybeUploadAccessorySnapshot(MultipartFile accessorySnapshot, QCReport report) {
        if (accessorySnapshot == null || accessorySnapshot.isEmpty()) {
            return;
        }
        Long taskId = null;
        if (report.getTask() != null) {
            taskId = report.getTask().getTaskId();
        }
        String uploadedUrl = imageStorageService.uploadQcAccessorySnapshot(accessorySnapshot, taskId);
        report.setAccessorySnapShotUrl(uploadedUrl);
    }

    private void validatePhaseAndResult(QCPhase phase, QCResult result) {
        if (phase == QCPhase.PRE_RENTAL) {
            if (result != QCResult.READY_FOR_SHIPPING && result != QCResult.PRE_RENTAL_FAILED) {
                throw new IllegalArgumentException("Kết quả QC không hợp lệ cho giai đoạn PRE_RENTAL");
            }
        } else if (phase == QCPhase.POST_RENTAL) {
            if (result != QCResult.READY_FOR_RE_STOCK && result != QCResult.POST_RENTAL_FAILED) {
                throw new IllegalArgumentException("Kết quả QC không hợp lệ cho giai đoạn POST_RENTAL");
            }
        } else {
            throw new IllegalArgumentException("Giai đoạn QC không được hỗ trợ: " + phase);
        }
    }

    private Account getAccountByUsername(String username) {
        return accountService.getByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản với tên đăng nhập: " + username));
    }

    private Task getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy công việc với id: " + taskId));
    }

    private boolean isStaffAssignedToTask(Task task, Staff staff) {
        if (task == null || staff == null) {
            return false;
        }
        if (task.getAssignedStaff() == null || task.getAssignedStaff().isEmpty()) {
            return false;
        }
        return task.getAssignedStaff().stream()
                .anyMatch(assigned -> assigned != null && staff.getStaffId().equals(assigned.getStaffId()));
    }

    private QcReportDtoContext prepareQcReportDtoContext(QCReport report) {
        List<Allocation> allocations = resolveAllocations(report);
        if (!CollectionUtils.isEmpty(allocations)) {
            allocations.stream()
                    .filter(Objects::nonNull)
                    .forEach(this::ensureQcBaselineSnapshotsLoaded);
        }
        List<DiscrepancyReport> discrepancies = findReportDiscrepancies(report);
        return new QcReportDtoContext(allocations, discrepancies);
    }

    private Long resolveOrderId(QCReport report, List<Allocation> allocations) {
        if (!CollectionUtils.isEmpty(allocations)) {
            Long orderId = allocations.stream()
                    .map(Allocation::getOrderDetail)
                    .filter(Objects::nonNull)
                    .map(OrderDetail::getRentalOrder)
                    .filter(Objects::nonNull)
                    .map(RentalOrder::getOrderId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (orderId != null) {
                return orderId;
            }
        }
        RentalOrder rentalOrder = report.getRentalOrder();
        if (rentalOrder != null && rentalOrder.getOrderId() != null) {
            return rentalOrder.getOrderId();
        }
        Task task = report.getTask();
        return task != null ? task.getOrderId() : null;
    }

    private List<DiscrepancyReport> findReportDiscrepancies(QCReport report) {
        Long reportId = Optional.ofNullable(report)
                .map(QCReport::getQcReportId)
                .orElse(null);
        if (reportId == null) {
            return List.of();
        }
        return discrepancyReportRepository.findByCreatedFromAndRefIdOrderByCreatedAtDesc(
                DiscrepancyCreatedFrom.QC_REPORT,
                reportId
        );
    }

    private RentalOrder resolveRentalOrderFromDetails(Map<OrderDetail, List<String>> resolvedDetails) {
        if (resolvedDetails == null || resolvedDetails.isEmpty()) {
            return null;
        }
        return resolvedDetails.keySet().stream()
                .filter(Objects::nonNull)
                .map(OrderDetail::getRentalOrder)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private RentalOrder determineRentalOrder(Task task, Map<OrderDetail, List<String>> resolvedDetails) {
        RentalOrder rentalOrder = resolveRentalOrderFromDetails(resolvedDetails);
        if (rentalOrder != null) {
            return rentalOrder;
        }
        return findRentalOrderByTask(task);
    }

    private RentalOrder resolveRentalOrderForReport(QCReport report) {
        if (report == null) {
            return null;
        }
        RentalOrder rentalOrder = report.getRentalOrder();
        if (rentalOrder != null) {
            return rentalOrder;
        }
        return findRentalOrderByTask(report.getTask());
    }

    private RentalOrder findRentalOrderByTask(Task task) {
        if (task == null || task.getOrderId() == null) {
            return null;
        }
        return rentalOrderRepository.findById(task.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng với id: " + task.getOrderId()));
    }

    private void ensureRentalOrderProcessing(RentalOrder rentalOrder, Task task) {
        if (rentalOrder == null) {
            throw new IllegalStateException("Không xác định được đơn hàng gắn với báo cáo QC");
        }
        
        // Nếu task này là replacement task (gắn với complaint) → cho phép QC ngay cả khi order không ở PROCESSING
        // Vì complaint có thể xảy ra sau khi đã giao hàng (order ở ACTIVE/RENTED)
        if (task != null && task.getTaskId() != null) {
            List<CustomerComplaint> complaints = customerComplaintRepository.findByReplacementTask_TaskId(task.getTaskId());
            if (complaints != null && !complaints.isEmpty()) {
                // Đây là replacement task → bỏ qua validation order status
                log.debug("Task {} là replacement task cho complaint, cho phép PRE_RENTAL QC ngay cả khi order không ở PROCESSING", task.getTaskId());
                return;
            }
        }
        
        // Validation bình thường cho PRE_RENTAL QC của order mới
        OrderStatus status = rentalOrder.getOrderStatus();
        if (status != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Không thể tạo/cập nhật báo cáo QC khi đơn hàng không ở trạng thái PROCESSING");
        }
    }

    private List<Allocation> resolveAllocations(QCReport report) {
        Long orderId = Optional.ofNullable(report)
                .map(QCReport::getRentalOrder)
                .map(RentalOrder::getOrderId)
                .orElse(null);
        if (orderId != null) {
            return allocationRepository.findByOrderDetail_RentalOrder_OrderId(orderId);
        }
        if (report.getQcReportId() != null) {
            return allocationRepository.findByQcReport_QcReportId(report.getQcReportId());
        }
        return report.getAllocations() != null ? report.getAllocations() : List.of();
    }


    private void ensureQcBaselineSnapshotsLoaded(Allocation allocation) {
        if (allocation == null) {
            return;
        }
        boolean hasQcSnapshots = allocation.getBaselineSnapshots() != null
                && allocation.getBaselineSnapshots().stream()
                .anyMatch(snapshot -> snapshot != null && snapshot.getSource() == AllocationSnapshotSource.QC_BEFORE);
        if (hasQcSnapshots) {
            return;
        }
        Long allocationId = allocation.getAllocationId();
        if (allocationId == null) {
            return;
        }
        List<AllocationConditionSnapshot> snapshots = allocationConditionSnapshotRepository
                .findByAllocation_AllocationIdAndSnapshotType(allocationId, AllocationSnapshotType.BASELINE)
                .stream()
                .filter(snapshot -> snapshot.getSource() == AllocationSnapshotSource.QC_BEFORE)
                .collect(Collectors.toList());
        if (snapshots.isEmpty()) {
            return;
        }
        if (allocation.getBaselineSnapshots() == null) {
            allocation.setBaselineSnapshots(new ArrayList<>());
        }
        allocation.getBaselineSnapshots().removeIf(snapshot ->
                snapshot != null && snapshot.getSource() == AllocationSnapshotSource.QC_BEFORE);
        allocation.getBaselineSnapshots().addAll(snapshots);
    }


    private void extendReservationHold(Task task, List<Allocation> allocations, QCResult result) {
        if (result != QCResult.READY_FOR_SHIPPING) {
            return;
        }
        Long orderId = Optional.ofNullable(task)
                .map(Task::getOrderId)
                .orElseGet(() -> allocations == null ? null : allocations.stream()
                        .map(Allocation::getOrderDetail)
                        .filter(Objects::nonNull)
                        .map(OrderDetail::getRentalOrder)
                        .filter(Objects::nonNull)
                        .map(RentalOrder::getOrderId)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null));
        if (orderId != null) {
            reservationService.moveToUnderReview(orderId);
        }
    }

    /**
     * Xử lý khi PRE_RENTAL QC failed:
     * - Reset device status về AVAILABLE
     * - Reset complaint suggested device về AVAILABLE nếu là replacement task
     */
    private void handlePreRentalQcFailed(Task task, Map<OrderDetail, List<String>> orderDetailSerials) {
        if (task == null || task.getTaskId() == null) {
            return;
        }

        // Reset device status về AVAILABLE
        Set<Device> devicesToReset = new LinkedHashSet<>();
        if (orderDetailSerials != null && !orderDetailSerials.isEmpty()) {
            orderDetailSerials.forEach((orderDetail, serialNumbers) -> {
                if (serialNumbers != null) {
                    serialNumbers.forEach(serial -> {
                        if (serial != null && !serial.isBlank()) {
                            deviceRepository.findBySerialNumber(serial).ifPresent(device -> {
                                if (device.getStatus() == DeviceStatus.PRE_RENTAL_QC) {
                                    device.setStatus(DeviceStatus.AVAILABLE);
                                    devicesToReset.add(device);
                                }
                            });
                        }
                    });
                }
            });
        }

        // Reset complaint suggested device nếu là replacement task
        List<CustomerComplaint> complaints = customerComplaintRepository.findByReplacementTask_TaskId(task.getTaskId());
        if (complaints != null && !complaints.isEmpty()) {
            for (CustomerComplaint complaint : complaints) {
                if (complaint != null && complaint.getReplacementDevice() != null) {
                    Device suggestedDevice = complaint.getReplacementDevice();
                    if (suggestedDevice.getStatus() == DeviceStatus.PRE_RENTAL_QC) {
                        suggestedDevice.setStatus(DeviceStatus.AVAILABLE);
                        devicesToReset.add(suggestedDevice);
                        log.info("Reset suggested device {} về AVAILABLE vì PRE_RENTAL QC failed cho complaint #{}",
                                suggestedDevice.getSerialNumber(), complaint.getComplaintId());
                    }
                }
            }
        }

        if (!devicesToReset.isEmpty()) {
            deviceRepository.saveAll(devicesToReset);
            deviceRepository.flush();
            log.info("Đã reset {} device về AVAILABLE sau khi PRE_RENTAL QC failed cho task {}", devicesToReset.size(), task.getTaskId());
        }
    }

    /**
     * Update complaint sau khi allocation được tạo từ QC report PRE_RENTAL
     * Tự động update replacementDevice và replacementAllocation từ allocation mới được tạo
     * Đóng allocation cũ và tạo biên bản replacement report
     */
    private void updateComplaintAfterQcAllocation(Task task, List<Allocation> newAllocations) {
        if (task == null || task.getTaskId() == null || newAllocations == null || newAllocations.isEmpty()) {
            return;
        }

        // Tìm complaint có replacementTask = task này
        List<CustomerComplaint> complaints = customerComplaintRepository.findByReplacementTask_TaskId(task.getTaskId());
        if (complaints == null || complaints.isEmpty()) {
            // Validation: Kiểm tra xem task description có chứa "Complaint" không
            // Nếu có nhưng không tìm thấy complaint → có thể bị lỗi hoặc task chưa được link đúng
            String taskDescription = task.getDescription() != null ? task.getDescription() : "";
            if (taskDescription.toLowerCase().contains("complaint") || 
                taskDescription.toLowerCase().contains("device thay thế") ||
                taskDescription.toLowerCase().contains("replacement")) {
                log.warn("⚠️ Task {} có description liên quan đến complaint nhưng không tìm thấy complaint nào có replacementTask = task này. " +
                        "Có thể task chưa được link đúng với complaint. Description: {}", 
                        task.getTaskId(), taskDescription);
            }
            return; // Không phải replacement task
        }

        // Lấy allocation đầu tiên (thường chỉ có 1 allocation cho replacement)
        Allocation newAllocation = newAllocations.stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (newAllocation == null || newAllocation.getDevice() == null || newAllocation.getOrderDetail() == null) {
            return;
        }

        Device newDevice = newAllocation.getDevice();

        // Update từng complaint
        for (CustomerComplaint complaint : complaints) {
            if (complaint == null || complaint.getComplaintId() == null) {
                continue;
            }

            // Lấy allocation cũ (nếu có) - từ suggested device
            Allocation oldSuggestedAllocation = complaint.getReplacementAllocation();
            if (oldSuggestedAllocation != null && oldSuggestedAllocation.getAllocationId() != null
                    && !oldSuggestedAllocation.getAllocationId().equals(newAllocation.getAllocationId())) {
                // Đóng allocation cũ của suggested device (nếu technician chọn device khác)
                oldSuggestedAllocation.setStatus("RETURNED");
                oldSuggestedAllocation.setReturnedAt(LocalDateTime.now());
                String oldNotes = oldSuggestedAllocation.getNotes() != null ? oldSuggestedAllocation.getNotes() : "";
                oldSuggestedAllocation.setNotes(oldNotes + (oldNotes.isEmpty() ? "" : "\n") +
                        "Device đề xuất không được chọn, technician đã chọn device khác (complaint #" + complaint.getComplaintId() + ")");
                allocationRepository.save(oldSuggestedAllocation);
            }

            // Reset suggested device về AVAILABLE nếu technician chọn device khác
            Device suggestedDevice = complaint.getReplacementDevice();
            if (suggestedDevice != null && suggestedDevice.getDeviceId() != null
                    && !suggestedDevice.getDeviceId().equals(newDevice.getDeviceId())
                    && suggestedDevice.getStatus() == DeviceStatus.PRE_RENTAL_QC) {
                // Technician đã chọn device khác, reset suggested device về AVAILABLE
                suggestedDevice.setStatus(DeviceStatus.AVAILABLE);
                deviceRepository.save(suggestedDevice);
                log.info("Reset suggested device {} về AVAILABLE vì technician đã chọn device khác {} cho complaint #{}",
                        suggestedDevice.getSerialNumber(), newDevice.getSerialNumber(), complaint.getComplaintId());
            }

            // Đóng allocation gốc của device hỏng (nếu chưa đóng)
            Allocation brokenDeviceAllocation = complaint.getAllocation();
            if (brokenDeviceAllocation != null && brokenDeviceAllocation.getAllocationId() != null
                    && !"RETURNED".equals(brokenDeviceAllocation.getStatus())) {
                brokenDeviceAllocation.setStatus("RETURNED");
                brokenDeviceAllocation.setReturnedAt(LocalDateTime.now());
                String brokenNotes = brokenDeviceAllocation.getNotes() != null ? brokenDeviceAllocation.getNotes() : "";
                brokenDeviceAllocation.setNotes(brokenNotes + (brokenNotes.isEmpty() ? "" : "\n") +
                        "Thiết bị hỏng, đã thay thế bằng device " + newDevice.getSerialNumber() + " (complaint #" + complaint.getComplaintId() + ")");
                allocationRepository.save(brokenDeviceAllocation);
            }

            // Update complaint với device và allocation mới (device technician đã chọn)
            complaint.setReplacementDevice(newDevice);
            complaint.setReplacementAllocation(newAllocation);
            customerComplaintRepository.save(complaint);

            // Tạo biên bản replacement report (nếu chưa có)
            try {
                com.rentaltech.techrental.staff.model.DeviceReplacementReport existingReport = complaint.getReplacementReport();
                if (existingReport == null || existingReport.getReplacementReportId() == null) {
                    // Tìm staff từ task hoặc lấy từ QC report creator
                    String createdBy = task.getAssignedStaff() != null && !task.getAssignedStaff().isEmpty()
                            ? task.getAssignedStaff().stream()
                                .filter(Objects::nonNull)
                                .filter(s -> s.getAccount() != null)
                                .map(s -> s.getAccount().getUsername())
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElse(null)
                            : null;
                    if (createdBy == null) {
                        // Fallback: lấy từ QC report creator hoặc system
                        createdBy = "system";
                    }
                    deviceReplacementReportService.createDeviceReplacementReport(complaint.getComplaintId(), createdBy);
                }
            } catch (Exception ex) {
                log.warn("Không thể tạo biên bản replacement report cho complaint {}: {}", complaint.getComplaintId(), ex.getMessage());
                // Không throw để không làm gián đoạn flow QC
            }
            
            // Tạo task "Device Replacement" để operator assign cho staff đi giao
            try {
                RentalOrder complaintOrder = complaint.getRentalOrder();
                Device complaintBrokenDevice = complaint.getDevice();
                if (complaintOrder != null && complaintBrokenDevice != null) {
                    log.info("Bắt đầu tạo task Device Replacement cho complaint #{} (order #{})", 
                            complaint.getComplaintId(), complaintOrder.getOrderId());
                    createDeviceReplacementTask(complaint, newDevice, complaintBrokenDevice, complaintOrder);
                    log.info("Hoàn thành tạo task Device Replacement cho complaint #{}", complaint.getComplaintId());
                } else {
                    log.warn("Không thể tạo task Device Replacement: complaintOrder={}, complaintBrokenDevice={}", 
                            complaintOrder, complaintBrokenDevice);
                }
            } catch (Exception ex) {
                log.error("Lỗi khi tạo task Device Replacement cho complaint {}: {}", 
                        complaint.getComplaintId(), ex.getMessage(), ex);
                // Không throw để không làm gián đoạn flow QC
            }
        }
    }

    /**
     * Tạo task "Device Replacement" sau khi QC pass cho complaint
     * Task này để operator assign cho staff đi giao device mới cho khách hàng
     */
    private void createDeviceReplacementTask(CustomerComplaint complaint, 
                                             Device newDevice, 
                                             Device brokenDevice,
                                             RentalOrder order) {
        if (complaint == null || complaint.getComplaintId() == null || order == null) {
            return;
        }
        
        // Kiểm tra xem đã có task "Device Replacement" cho order này chưa
        // (có thể có nhiều complaints cùng order, nhưng chỉ cần 1 task delivery cho order)
        Task existingDeliveryTask = taskRepository.findByOrderId(order.getOrderId()).stream()
                .filter(task -> {
                    String categoryName = task.getTaskCategory() != null ? task.getTaskCategory().getName() : null;
                    return "Device Replacement".equalsIgnoreCase(categoryName) 
                            && task.getStatus() == TaskStatus.PENDING;
                })
                .findFirst()
                .orElse(null);
        
        if (existingDeliveryTask != null) {
            log.debug("Đã có task Device Replacement pending cho order {}, không tạo mới", order.getOrderId());
            return;
        }
        
        // Tìm TaskCategory "Device Replacement"
        TaskCategory deliveryCategory = taskCategoryRepository.findByName("Device Replacement")
                .orElse(null);
        
        if (deliveryCategory == null) {
            log.warn("Không tìm thấy TaskCategory 'Device Replacement'. Vui lòng tạo category này trước.");
            return;
        }
        
        // Tạo task Device Replacement
        TaskCreateRequestDto deliveryTaskRequest = TaskCreateRequestDto.builder()
                .taskCategoryId(deliveryCategory.getTaskCategoryId())
                .orderId(order.getOrderId())
                .assignedStaffIds(null) // Không assign cho ai, để operator assign sau
                .description(String.format("Giao device thay thế cho khách hàng (Complaint #%d, Order #%d). " +
                        "Device hỏng: %s (Serial: %s). Device thay thế: %s (Serial: %s). " +
                        "Vui lòng assign staff đi giao device mới và ký biên bản replacement report.",
                        complaint.getComplaintId(),
                        order.getOrderId(),
                        brokenDevice.getDeviceModel() != null ? brokenDevice.getDeviceModel().getDeviceName() : "N/A",
                        brokenDevice.getSerialNumber(),
                        newDevice.getDeviceModel() != null ? newDevice.getDeviceModel().getDeviceName() : "N/A",
                        newDevice.getSerialNumber()))
                .plannedStart(LocalDateTime.now())
                .plannedEnd(LocalDateTime.now().plusHours(24)) // Deadline 24h cho delivery
                .build();
        
        try {
            log.debug("Gọi taskService.createTask() để tạo task Device Replacement. OrderId: {}, ComplaintId: {}", 
                    order.getOrderId(), complaint.getComplaintId());
            Task deliveryTask = taskService.createTask(deliveryTaskRequest, "system");
            log.info("✅ Đã tạo task Device Replacement (taskId: {}) cho complaint #{} và order #{}", 
                    deliveryTask.getTaskId(), complaint.getComplaintId(), order.getOrderId());
        } catch (Exception ex) {
            log.error("❌ Lỗi khi tạo task Device Replacement cho complaint #{} và order #{}: {}", 
                    complaint.getComplaintId(), order.getOrderId(), ex.getMessage(), ex);
        }
    }

    /**
     * Validation: Kiểm tra xem task có phải là replacement task cho complaint không
     * Dựa vào category name "Pre rental QC Replace" hoặc description có chứa "Complaint"
     */
    private void validateTaskForComplaint(Task task) {
        if (task == null || task.getTaskId() == null) {
            return;
        }

        String categoryName = task.getTaskCategory() != null ? task.getTaskCategory().getName() : null;
        boolean isReplaceCategory = "Pre rental QC Replace".equalsIgnoreCase(categoryName);
        
        String taskDescription = task.getDescription() != null ? task.getDescription() : "";
        boolean hasComplaintKeywords = taskDescription.toLowerCase().contains("complaint") ||
                taskDescription.toLowerCase().contains("device thay thế") ||
                taskDescription.toLowerCase().contains("replacement");

        // Nếu là category "Pre rental QC Replace" → phải có complaint link
        if (isReplaceCategory) {
            List<CustomerComplaint> complaints = customerComplaintRepository.findByReplacementTask_TaskId(task.getTaskId());
            if (complaints == null || complaints.isEmpty()) {
                log.warn("⚠️ Task {} có category 'Pre rental QC Replace' nhưng không tìm thấy complaint nào có replacementTask = task này. " +
                        "Có thể task chưa được link đúng với complaint. " +
                        "Description: {}",
                        task.getTaskId(), taskDescription);
            } else {
                log.debug("✅ Task {} (category: 'Pre rental QC Replace') được validate: tìm thấy {} complaint(s)",
                        task.getTaskId(), complaints.size());
            }
        } else if (hasComplaintKeywords) {
            // Nếu không phải Replace category nhưng có keywords → cảnh báo
            List<CustomerComplaint> complaints = customerComplaintRepository.findByReplacementTask_TaskId(task.getTaskId());
            if (complaints == null || complaints.isEmpty()) {
                log.warn("⚠️ Task {} có description liên quan đến complaint nhưng category là '{}' (không phải 'Pre rental QC Replace'). " +
                        "Có thể đây là task QC ban đầu bị nhầm lẫn. " +
                        "Description: {}. " +
                        "Nếu đây là task QC cho complaint, vui lòng kiểm tra lại category.",
                        task.getTaskId(), categoryName, taskDescription);
            }
        }
    }

    private record ConditionSnapshotPayload(Long conditionDefinitionId,
                                            String conditionDefinitionName,
                                            String severity,
                                            List<String> images,
                                            boolean shouldPersist) {
    }
}
