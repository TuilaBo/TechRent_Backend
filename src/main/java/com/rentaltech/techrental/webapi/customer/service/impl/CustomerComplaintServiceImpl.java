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
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.staff.service.taskservice.TaskService;
import com.rentaltech.techrental.webapi.customer.model.ComplaintStatus;
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
    private final StaffService staffService;
    private final AccountService accountService;
    private final BookingCalendarService bookingCalendarService;
    private final BookingCalendarRepository bookingCalendarRepository;

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
    public CustomerComplaintResponseDto processComplaint(Long complaintId, String staffNote, String username) {
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
                order.getStartDate(),  // Check từ startDate của order
                order.getEndDate()     // Đến endDate của order
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

        // Tạo Task để staff đi đổi máy
        TaskCategory replacementCategory = taskCategoryRepository.findByNameIgnoreCase("Device Replacement")
                .orElseGet(() -> taskCategoryRepository.findByNameIgnoreCase("Delivery")
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy TaskCategory cho device replacement")));

        // Tìm staff phù hợp (CUSTOMER_SUPPORT_STAFF hoặc TECHNICIAN)
        List<Staff> availableStaff = findAvailableSupportStaff();
        Set<Staff> assignedStaff = availableStaff.isEmpty() 
                ? Set.of(staff) 
                : Set.of(availableStaff.get(0));

        TaskCreateRequestDto taskRequest = TaskCreateRequestDto.builder()
                .taskCategoryId(replacementCategory.getTaskCategoryId())
                .orderId(order.getOrderId())
                .assignedStaffIds(assignedStaff.stream().map(Staff::getStaffId).collect(Collectors.toList()))
                .description(String.format("Thay thế thiết bị %s (Serial: %s) bằng thiết bị %s (Serial: %s) cho đơn hàng #%d. Khiếu nại #%d",
                        brokenDevice.getDeviceModel().getDeviceName(),
                        brokenDevice.getSerialNumber(),
                        replacementDevice.getDeviceModel().getDeviceName(),
                        replacementDevice.getSerialNumber(),
                        order.getOrderId(),
                        complaintId))
                .plannedStart(LocalDateTime.now())
                .plannedEnd(LocalDateTime.now().plusHours(24)) // Deadline 24h
                .build();

        Task replacementTask = taskService.createTask(taskRequest, username);

        // Update complaint
        complaint.setStatus(ComplaintStatus.PROCESSING);
        complaint.setStaffNote(staffNote);
        complaint.setReplacementDevice(replacementDevice);
        complaint.setReplacementTask(replacementTask);
        complaint.setReplacementAllocation(savedAllocation);
        complaint.setProcessedAt(LocalDateTime.now());

        CustomerComplaint saved = complaintRepository.save(complaint);
        return CustomerComplaintResponseDto.from(saved);
    }

    @Override
    public CustomerComplaintResponseDto resolveComplaint(Long complaintId, String staffNote, String username) {
        CustomerComplaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khiếu nại: " + complaintId));

        if (complaint.getStatus() != ComplaintStatus.PROCESSING) {
            throw new IllegalStateException("Chỉ có thể đánh dấu giải quyết khiếu nại ở trạng thái PROCESSING");
        }

        Account account = accountService.getByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản"));
        Staff staff = staffService.getStaffByAccountIdOrThrow(account.getAccountId());

        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResolvedAt(LocalDateTime.now());
        complaint.setResolvedBy(staff);
        if (staffNote != null && !staffNote.isBlank()) {
            complaint.setStaffNote(staffNote);
        }

        CustomerComplaint saved = complaintRepository.save(complaint);
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

    /**
     * Tìm staff phù hợp để assign task (CUSTOMER_SUPPORT_STAFF hoặc TECHNICIAN)
     */
    private List<Staff> findAvailableSupportStaff() {
        // Tìm CUSTOMER_SUPPORT_STAFF trước
        List<Staff> supportStaffs = staffService.getStaffByRole(
                com.rentaltech.techrental.staff.model.StaffRole.CUSTOMER_SUPPORT_STAFF
        ).stream()
                .filter(staff -> staff.getIsActive() != null && staff.getIsActive())
                .collect(Collectors.toList());

        if (!supportStaffs.isEmpty()) {
            return supportStaffs;
        }

        // Fallback: TECHNICIAN
        List<Staff> technicians = staffService.getStaffByRole(
                com.rentaltech.techrental.staff.model.StaffRole.TECHNICIAN
        ).stream()
                .filter(staff -> staff.getIsActive() != null && staff.getIsActive())
                .collect(Collectors.toList());

        return technicians;
    }
}

