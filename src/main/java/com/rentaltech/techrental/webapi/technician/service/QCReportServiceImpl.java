package com.rentaltech.techrental.webapi.technician.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.model.OrderDetail;
import com.rentaltech.techrental.webapi.customer.repository.OrderDetailRepository;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import com.rentaltech.techrental.webapi.technician.model.QCPhase;
import com.rentaltech.techrental.webapi.technician.model.QCReport;
import com.rentaltech.techrental.webapi.technician.model.QCResult;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportCreateRequestDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportResponseDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportUpdateRequestDto;
import com.rentaltech.techrental.webapi.technician.repository.QCReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QCReportServiceImpl implements QCReportService {

    private final QCReportRepository qcReportRepository;
    private final TaskRepository taskRepository;
    private final DeviceRepository deviceRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final AllocationRepository allocationRepository;
    private final AccountService accountService;
    private final StaffService staffService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public QCReportResponseDto createReport(QCReportCreateRequestDto request, String username) {
        Account account = getAccountByUsername(username);
        Task task = getTaskById(request.getTaskId());
        Staff assignedStaff = task.getAssignedStaff();
        Staff currentStaff = staffService.getStaffByAccountIdOrThrow(account.getAccountId());

        if (assignedStaff == null || !assignedStaff.getStaffId().equals(currentStaff.getStaffId())) {
            throw new AccessDeniedException("Kỹ thuật viên không được phân công cho công việc này");
        }

        qcReportRepository.findByTask_TaskId(task.getTaskId())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Đã tồn tại báo cáo QC cho công việc này");
                });

        validatePhaseAndResult(request.getPhase(), request.getResult());
        Map<OrderDetail, List<String>> orderDetailSerials = resolveOrderDetails(task, request.getOrderDetailSerialNumbers());

        QCReport qcReport = QCReport.builder()
                .phase(request.getPhase())
                .result(request.getResult())
                .findings(request.getFindings())
                .accessorySnapShotUrl(request.getAccessorySnapShotUrl())
                .createdBy(username)
                .task(task)
                .build();

        QCReport saved = qcReportRepository.save(qcReport);

        List<Allocation> allocations = createAllocationsIfNeeded(saved, orderDetailSerials);
        if (!allocations.isEmpty()) {
            saved.setAllocations(new ArrayList<>(allocations));
        }

        markTaskCompleted(task);
        maybeNotifyOrderConfirmed(saved);
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public QCReportResponseDto getReport(Long reportId) {
        QCReport report = qcReportRepository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy báo cáo QC với id: " + reportId));
        return mapToResponseDto(report);
    }

    @Override
    @Transactional
    public QCReportResponseDto updateReport(Long reportId, QCReportUpdateRequestDto request, String username) {
        QCReport report = qcReportRepository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy báo cáo QC với id: " + reportId));

        Account account = getAccountByUsername(username);
        boolean isAdmin = account.getRole() == Role.ADMIN;

        if (!isAdmin) {
            Staff currentStaff = staffService.getStaffByAccountIdOrThrow(account.getAccountId());
            Staff assignedStaff = report.getTask().getAssignedStaff();
            if (assignedStaff == null || !assignedStaff.getStaffId().equals(currentStaff.getStaffId())) {
                throw new AccessDeniedException("Kỹ thuật viên không được phân công cho công việc này");
            }
        }

        validatePhaseAndResult(request.getPhase(), request.getResult());

        report.setPhase(request.getPhase());
        report.setResult(request.getResult());
        report.setFindings(request.getFindings());
        report.setAccessorySnapShotUrl(request.getAccessorySnapShotUrl());

        if (report.getResult() == QCResult.READY_FOR_SHIPPING) {
            Map<Long, List<String>> serialNumbersByOrderDetail = request.getOrderDetailSerialNumbers();
            if (serialNumbersByOrderDetail != null) {
                Map<OrderDetail, List<String>> resolved = resolveOrderDetails(report.getTask(), serialNumbersByOrderDetail);
                if (resolved.isEmpty()) {
                    throw new IllegalArgumentException("Cần cung cấp danh sách thiết bị để tạo allocation");
                }
                clearAllocations(report);
                List<Allocation> allocations = createAllocations(report, resolved);
                report.setAllocations(new ArrayList<>(allocations));
            } else if (report.getAllocations() == null || report.getAllocations().isEmpty()) {
                throw new IllegalArgumentException("Cần cung cấp danh sách thiết bị khi kết quả READY_FOR_SHIPPING");
            }
        } else {
            clearAllocations(report);
        }

        QCReport updated = qcReportRepository.save(report);
        maybeNotifyOrderConfirmed(updated);
        return mapToResponseDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCReportResponseDto> getReportsByOrder(Long rentalOrderId) {
        List<Task> qcTasks = taskRepository.findByOrderId(rentalOrderId).stream()
                .filter(task -> {
                    String type = task.getType();
                    return type != null && (type.equalsIgnoreCase("PRE_RENTAL_QC") || type.equalsIgnoreCase("POST_RENTAL_QC"));
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
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private List<Allocation> createAllocationsIfNeeded(QCReport report, Map<OrderDetail, List<String>> orderDetailSerials) {
        if (report.getResult() != QCResult.READY_FOR_SHIPPING) {
            return List.of();
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
        }
        return allocationRepository.saveAll(allocations);
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

    private void clearAllocations(QCReport report) {
        Set<Device> devicesToRelease = new LinkedHashSet<>();
        if (report.getQcReportId() != null) {
            List<Allocation> existing = allocationRepository.findByQcReport_QcReportId(report.getQcReportId());
            if (!existing.isEmpty()) {
                existing.stream()
                        .map(Allocation::getDevice)
                        .filter(Objects::nonNull)
                        .forEach(devicesToRelease::add);
                allocationRepository.deleteAll(existing);
            }
        }

        if (report.getAllocations() != null) {
            report.getAllocations().stream()
                    .map(Allocation::getDevice)
                    .filter(Objects::nonNull)
                    .forEach(devicesToRelease::add);
            report.getAllocations().clear();
        } else {
            report.setAllocations(new ArrayList<>());
        }

        if (!devicesToRelease.isEmpty()) {
            devicesToRelease.forEach(device -> device.setStatus(DeviceStatus.AVAILABLE));
            deviceRepository.saveAll(devicesToRelease);
        }
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

    private QCReportResponseDto mapToResponseDto(QCReport report) {
        Task task = report.getTask();

        List<Allocation> allocations = resolveAllocations(report);
        OrderDetail orderDetail = allocations.stream()
                .map(Allocation::getOrderDetail)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        Long orderId = null;
        if (orderDetail != null && orderDetail.getRentalOrder() != null) {
            orderId = orderDetail.getRentalOrder().getOrderId();
        } else if (task != null) {
            orderId = task.getOrderId();
        }

        List<com.rentaltech.techrental.device.model.dto.DeviceResponseDto> deviceDtos = allocations.stream()
                .map(Allocation::getDevice)
                .filter(Objects::nonNull)
                .map(this::toDeviceDto)
                .collect(Collectors.toList());

        return QCReportResponseDto.builder()
                .qcReportId(report.getQcReportId())
                .phase(report.getPhase())
                .result(report.getResult())
                .findings(report.getFindings())
                .accessorySnapShotUrl(report.getAccessorySnapShotUrl())
                .createdAt(report.getCreatedAt())
                .createdBy(report.getCreatedBy())
                .taskId(task != null ? task.getTaskId() : null)
                .orderDetailId(orderDetail != null ? orderDetail.getOrderDetailId() : null)
                .orderId(orderId)
                .devices(deviceDtos)
                .build();
    }

    private List<Allocation> resolveAllocations(QCReport report) {
        if (report.getQcReportId() != null) {
            return allocationRepository.findByQcReport_QcReportId(report.getQcReportId());
        }
        return report.getAllocations() != null ? report.getAllocations() : List.of();
    }

    private com.rentaltech.techrental.device.model.dto.DeviceResponseDto toDeviceDto(Device device) {
        if (device == null) {
            return null;
        }
        return com.rentaltech.techrental.device.model.dto.DeviceResponseDto.builder()
                .deviceId(device.getDeviceId())
                .serialNumber(device.getSerialNumber())
                .acquireAt(device.getAcquireAt())
                .status(device.getStatus())
                .deviceModelId(device.getDeviceModel() != null ? device.getDeviceModel().getDeviceModelId() : null)
                .build();
    }

    private void maybeNotifyOrderConfirmed(QCReport report) {
        if (report.getPhase() == QCPhase.PRE_RENTAL && report.getResult() == QCResult.READY_FOR_SHIPPING) {
            OrderDetail orderDetail = resolveAllocations(report).stream()
                    .map(Allocation::getOrderDetail)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (orderDetail != null && orderDetail.getRentalOrder() != null
                    && orderDetail.getRentalOrder().getCustomer() != null
                    && orderDetail.getRentalOrder().getCustomer().getCustomerId() != null) {
                Long orderId = orderDetail.getRentalOrder().getOrderId();
                notificationService.notifyCustomer(
                        orderDetail.getRentalOrder().getCustomer().getCustomerId(),
                        NotificationType.ORDER_CONFIRMED,
                        "Đơn hàng đã được xác nhận",
                        "Đơn hàng #" + orderId + " đã hoàn tất kiểm tra và sẵn sàng giao."
                );
            }
        }
    }
}
