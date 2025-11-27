package com.rentaltech.techrental.webapi.technician.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.service.AccountService;
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
import com.rentaltech.techrental.device.model.DiscrepancyType;
import com.rentaltech.techrental.device.model.dto.DiscrepancyInlineRequestDto;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportRequestDto;
import com.rentaltech.techrental.device.repository.AllocationConditionSnapshotRepository;
import com.rentaltech.techrental.device.repository.AllocationRepository;
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
import com.rentaltech.techrental.rentalorder.service.ReservationService;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import com.rentaltech.techrental.webapi.technician.model.QCPhase;
import com.rentaltech.techrental.webapi.technician.model.QCReport;
import com.rentaltech.techrental.webapi.technician.model.QCResult;
import com.rentaltech.techrental.webapi.technician.model.dto.QCDeviceConditionRequestDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportPostRentalCreateRequestDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportPostRentalUpdateRequestDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportPreRentalCreateRequestDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportPreRentalUpdateRequestDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportResponseDto;
import com.rentaltech.techrental.webapi.technician.repository.QCReportRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
            ensureRentalOrderProcessing(rentalOrder);
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

        List<Allocation> allocations = createAllocationsIfNeeded(saved, orderDetailSerials);
        if (!allocations.isEmpty()) {
            saved.setAllocations(new ArrayList<>(allocations));
            bookingCalendarService.createBookingsForAllocations(allocations);
            extendReservationHold(task, allocations, saved.getResult());
        }
        if (phase == QCPhase.PRE_RENTAL) {
            createBaselineSnapshots(allocations, deviceConditions, currentStaff);
        }
        if (!CollectionUtils.isEmpty(discrepancies)) {
            handleDiscrepancies(discrepancies, DiscrepancyCreatedFrom.QC_REPORT, saved.getQcReportId());
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
            ensureRentalOrderProcessing(rentalOrder);
        }
        report.setRentalOrder(rentalOrder);

        validatePhaseAndResult(phase, result);

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
        handleDiscrepancies(discrepancies, DiscrepancyCreatedFrom.QC_REPORT, updated.getQcReportId());
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
            ensureRentalOrderProcessing(rentalOrder);
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
    }

    private void handlePostRentalUpdate(QCReport report, QCResult result) {
        if (result == QCResult.READY_FOR_RE_STOCK) {
            updateDevicesAfterPostRentalQc(report);
        }
        // TODO: handle POST_RENTAL updates for other results
    }

    private void handleDiscrepancies(List<DiscrepancyInlineRequestDto> discrepancies,
                                     DiscrepancyCreatedFrom createdFrom,
                                     Long refId) {
        if (refId == null || discrepancies == null || discrepancies.isEmpty()) {
            return;
        }
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
                                .customerNote(dto.getCustomerNote())
                                .build()
                ));
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
                    return type != null && (type.equalsIgnoreCase("Pre rental QC") || type.equalsIgnoreCase("Post rental QC"));
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
        if (CollectionUtils.isEmpty(deviceConditions)) {
            allocationSnapshotService.createSnapshots(
                    allocations,
                    AllocationSnapshotType.BASELINE,
                    AllocationSnapshotSource.QC_BEFORE,
                    staff);
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
        Map<Long, List<QCDeviceConditionRequestDto>> conditionsByDeviceId = deviceConditions.stream()
                .filter(Objects::nonNull)
                .filter(condition -> condition.getDeviceId() != null && condition.getConditionDefinitionId() != null)
                .collect(Collectors.groupingBy(
                        QCDeviceConditionRequestDto::getDeviceId,
                        LinkedHashMap::new,
                        Collectors.toList()));
        if (conditionsByDeviceId.isEmpty()) {
            allocationSnapshotService.createSnapshots(
                    allocations,
                    AllocationSnapshotType.BASELINE,
                    AllocationSnapshotSource.QC_BEFORE,
                    staff);
            return;
        }
        Set<Long> conditionDefinitionIds = deviceConditions.stream()
                .filter(Objects::nonNull)
                .map(QCDeviceConditionRequestDto::getConditionDefinitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ConditionDefinition> conditionDefinitionsById = conditionDefinitionIds.isEmpty()
                ? Collections.emptyMap()
                : conditionDefinitionRepository.findAllById(conditionDefinitionIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ConditionDefinition::getConditionDefinitionId, Function.identity()));
        List<AllocationConditionSnapshot> snapshots = new ArrayList<>();
        conditionsByDeviceId.forEach((deviceId, conditions) -> {
            Allocation allocation = allocationsByDeviceId.get(deviceId);
            if (allocation == null) {
                log.warn("Không tìm thấy allocation tương ứng cho device {}", deviceId);
                return;
            }
            removeExistingQcBaselineSnapshots(allocation);
            for (QCDeviceConditionRequestDto condition : conditions) {
                if (condition == null || condition.getConditionDefinitionId() == null) {
                    continue;
                }
                ConditionDefinition conditionDefinition = conditionDefinitionsById.get(condition.getConditionDefinitionId());
                List<String> images = CollectionUtils.isEmpty(condition.getImages())
                        ? new ArrayList<>()
                        : new ArrayList<>(condition.getImages());
                AllocationConditionSnapshot snapshot = AllocationConditionSnapshot.builder()
                        .allocation(allocation)
                        .snapshotType(AllocationSnapshotType.BASELINE)
                        .source(AllocationSnapshotSource.QC_BEFORE)
                        .conditionDetails(List.of(AllocationConditionDetail.builder()
                                .conditionDefinitionId(condition.getConditionDefinitionId())
                                .conditionDefinitionName(conditionDefinition != null ? conditionDefinition.getName() : null)
                                .severity(condition.getSeverity())
                                .build()))
                        .images(images)
                        .staff(staff)
                        .build();
                snapshots.add(snapshot);
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

    private void ensureRentalOrderProcessing(RentalOrder rentalOrder) {
        if (rentalOrder == null) {
            throw new IllegalStateException("Không xác định được đơn hàng gắn với báo cáo QC");
        }
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
}
