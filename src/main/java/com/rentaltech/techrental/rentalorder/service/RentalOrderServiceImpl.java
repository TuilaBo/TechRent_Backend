package com.rentaltech.techrental.rentalorder.service;

import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.service.DeviceAllocationQueryService;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.model.dto.OrderDetailRequestDto;
import com.rentaltech.techrental.rentalorder.model.dto.OrderDetailResponseDto;
import com.rentaltech.techrental.rentalorder.model.dto.RentalOrderRequestDto;
import com.rentaltech.techrental.rentalorder.model.dto.RentalOrderResponseDto;
import com.rentaltech.techrental.rentalorder.repository.OrderDetailRepository;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.staff.service.PreRentalQcTaskCreator;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.KYCStatus;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class RentalOrderServiceImpl implements RentalOrderService {

    private static final Logger log = LoggerFactory.getLogger(RentalOrderServiceImpl.class);
    private static final String STAFF_NOTIFICATION_TOPIC_TEMPLATE = "/topic/staffs/%d/notifications";

    private final RentalOrderRepository rentalOrderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerRepository customerRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final PreRentalQcTaskCreator preRentalQcTaskCreator;
    private final BookingCalendarService bookingCalendarService;
    private final ReservationService reservationService;
    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final StaffService staffService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceAllocationQueryService deviceAllocationQueryService;

    @Override
    @Transactional(readOnly = true)
    public Page<RentalOrderResponseDto> search(String orderStatus, Long customerId, String shippingAddress, BigDecimal minTotalPrice, BigDecimal maxTotalPrice, BigDecimal minPricePerDay, BigDecimal maxPricePerDay, String startDateFrom, String startDateTo, String endDateFrom, String endDateTo, String createdAtFrom, String createdAtTo, Pageable pageable) {
        Long effectiveCustomerId = customerId;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean isCustomer = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_CUSTOMER"::equals);
            if (isCustomer) {
                var username = auth.getName();
                var customerOpt = customerRepository.findByAccount_Username(username);
                effectiveCustomerId = customerOpt.map(Customer::getCustomerId).orElse(-1L);
            }
        }

        Specification<RentalOrder> spec = buildSpecification(orderStatus, effectiveCustomerId, shippingAddress, minTotalPrice, maxTotalPrice, minPricePerDay, maxPricePerDay, startDateFrom, startDateTo, endDateFrom, endDateTo, createdAtFrom, createdAtTo);
        return rentalOrderRepository.findAll(spec, pageable).map(rentalOrder -> {
            List<OrderDetail> details = orderDetailRepository.findByRentalOrder_OrderId(rentalOrder.getOrderId());
            List<Device> allocatedDevices = deviceAllocationQueryService.getAllocatedDevicesForOrder(rentalOrder.getOrderId());
            return RentalOrderResponseDto.from(rentalOrder, details, allocatedDevices);
        });
    }

    @Override
    public RentalOrderResponseDto create(RentalOrderRequestDto request) {
        if (request == null) throw new IllegalArgumentException("RentalOrderRequestDto không được để trống");
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Cần cung cấp startDate và endDate");
        }
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (days <= 0) {
            throw new IllegalArgumentException("endDate phải sau startDate");
        }

        Authentication authCreate = SecurityContextHolder.getContext().getAuthentication();
        if (authCreate == null || !authCreate.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không được phép");
        }
        String usernameCreate = authCreate.getName();
        Customer customer = customerRepository.findByAccount_Username(usernameCreate)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng: " + usernameCreate));
        boolean kycVerified = customer.getKycStatus() == KYCStatus.VERIFIED;

        // Build details from device models and compute totals
        Computed computed = computeFromDetails(request);

        RentalOrder order = RentalOrder.builder()
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .shippingAddress(request.getShippingAddress())
                .orderStatus(kycVerified ? OrderStatus.PENDING : OrderStatus.PENDING_KYC)
                .depositAmount(computed.totalDeposit())
                .depositAmountHeld(BigDecimal.ZERO)
                .depositAmountUsed(BigDecimal.ZERO)
                .depositAmountRefunded(BigDecimal.ZERO)
                .totalPrice(computed.totalPerDay().multiply(BigDecimal.valueOf(days)))
                .pricePerDay(computed.totalPerDay())
                .customer(customer)
                .build();

        RentalOrder saved = rentalOrderRepository.save(order);

        // attach order to details then save
        for (OrderDetail od : computed.details()) od.setRentalOrder(saved);
        List<OrderDetail> persistedDetails = computed.details().isEmpty() ? List.of() : orderDetailRepository.saveAll(computed.details());
        reservationService.createPendingReservations(saved, persistedDetails);
        // Create allocations for each order detail
        // Create QC task linked to this order
        if (kycVerified) {
            preRentalQcTaskCreator.createIfNeeded(saved.getOrderId());
            notifyOperatorsOrderAndTaskCreated(saved);
        }

        List<Device> allocatedDevices = deviceAllocationQueryService.getAllocatedDevicesForOrder(saved.getOrderId());
        return RentalOrderResponseDto.from(saved, persistedDetails, allocatedDevices);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalOrderResponseDto findById(Long id) {
        RentalOrder order = rentalOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean isCustomer = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_CUSTOMER"::equals);
            if (isCustomer) {
                var username = auth.getName();
                var customerOpt = customerRepository.findByAccount_Username(username);
                Long requesterCustomerId = customerOpt.map(Customer::getCustomerId).orElse(-1L);
                Long ownerCustomerId = order.getCustomer() != null ? order.getCustomer().getCustomerId() : null;
                if (ownerCustomerId == null || !ownerCustomerId.equals(requesterCustomerId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền: đơn hàng không thuộc về bạn");
                }
            }
        }
        List<OrderDetail> details = orderDetailRepository.findByRentalOrder_OrderId(id);
        List<Device> allocatedDevices = deviceAllocationQueryService.getAllocatedDevicesForOrder(order.getOrderId());
        return RentalOrderResponseDto.from(order, details, allocatedDevices);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalOrderResponseDto> findAll() {
        List<RentalOrder> orders;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch("ROLE_CUSTOMER"::equals)) {
            var username = auth.getName();
            var customerOpt = customerRepository.findByAccount_Username(username);
            Long requesterCustomerId = customerOpt.map(Customer::getCustomerId).orElse(-1L);
            orders = rentalOrderRepository.findByCustomer_CustomerId(requesterCustomerId);
        } else {
            orders = rentalOrderRepository.findAll();
        }
        List<RentalOrderResponseDto> result = new ArrayList<>(orders.size());
        for (RentalOrder order : orders) {
            List<OrderDetail> details = orderDetailRepository.findByRentalOrder_OrderId(order.getOrderId());
            List<Device> allocatedDevices = deviceAllocationQueryService.getAllocatedDevicesForOrder(order.getOrderId());
            result.add(RentalOrderResponseDto.from(order, details, allocatedDevices));
        }
        return result;
    }

    @Override
    public RentalOrderResponseDto update(Long id, RentalOrderRequestDto request) {
        RentalOrder existing = rentalOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + id));

        if (request == null) throw new IllegalArgumentException("RentalOrderRequestDto không được để trống");
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Cần cung cấp startDate và endDate");
        }
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (days <= 0) {
            throw new IllegalArgumentException("endDate phải sau startDate");
        }

        Authentication authCreate = SecurityContextHolder.getContext().getAuthentication();
        if (authCreate == null || !authCreate.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không được phép");
        }
        String usernameCreate = authCreate.getName();
        Customer customer = customerRepository.findByAccount_Username(usernameCreate)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng: " + usernameCreate));
        boolean kycVerified = customer.getKycStatus() == KYCStatus.VERIFIED;

        Computed computed = computeFromDetails(request);

        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        existing.setShippingAddress(request.getShippingAddress());
        existing.setOrderStatus(kycVerified ? OrderStatus.PENDING : OrderStatus.PENDING_KYC);
        existing.setDepositAmount(computed.totalDeposit());
        existing.setDepositAmountHeld(BigDecimal.ZERO);
        existing.setDepositAmountUsed(BigDecimal.ZERO);
        existing.setDepositAmountRefunded(BigDecimal.ZERO);
        existing.setTotalPrice(computed.totalPerDay().multiply(BigDecimal.valueOf(days)));
        existing.setPricePerDay(computed.totalPerDay());
        existing.setCustomer(customer);

        RentalOrder saved = rentalOrderRepository.save(existing);

        reservationService.cancelReservations(id);
        orderDetailRepository.deleteByRentalOrder_OrderId(id);
        for (OrderDetail od : computed.details()) od.setRentalOrder(saved);
        List<OrderDetail> newDetails = computed.details().isEmpty() ? List.of() : orderDetailRepository.saveAll(computed.details());
        reservationService.createPendingReservations(saved, newDetails);
        if (kycVerified) {
            preRentalQcTaskCreator.createIfNeeded(saved.getOrderId());
        }
        // Create allocations for updated order details
        List<Device> allocatedDevices = deviceAllocationQueryService.getAllocatedDevicesForOrder(saved.getOrderId());
        return RentalOrderResponseDto.from(saved, newDetails, allocatedDevices);
    }

    @Override
    public RentalOrderResponseDto confirmReturn(Long id) {
        RentalOrder order = rentalOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không được phép");
        }
        boolean isCustomer = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_CUSTOMER"::equals);
        if (!isCustomer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ khách hàng được xác nhận trả hàng");
        }
        String username = auth.getName();
        Long requesterCustomerId = customerRepository.findByAccount_Username(username)
                .map(Customer::getCustomerId)
                .orElse(-1L);
        Long ownerCustomerId = order.getCustomer() != null ? order.getCustomer().getCustomerId() : null;
        if (ownerCustomerId == null || !ownerCustomerId.equals(requesterCustomerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Đơn hàng không thuộc về bạn");
        }
        if (order.getOrderStatus() != OrderStatus.IN_USE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ xác nhận trả hàng cho đơn đang sử dụng");
        }

        LocalDateTime plannedStart = order.getEndDate() != null
                ? order.getEndDate().minusHours(1)
                : LocalDateTime.now();
        LocalDateTime plannedEnd = plannedStart.plusHours(3);

        var category = taskCategoryRepository.findByNameIgnoreCase("Pick up rental order")
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy TaskCategory 'Pick up rental order'"));

        Task pickupTask = Task.builder()
                .taskCategory(category)
                .orderId(order.getOrderId())
                .description("Thu hồi thiết bị và hoàn tất thu đơn thuê #" + order.getOrderId() + ". Liên hệ khách để hẹn thời gian thu hồi.")
                .plannedStart(plannedStart)
                .plannedEnd(plannedEnd)
                .status(TaskStatus.PENDING)
                .build();
        Task savedTask = taskRepository.save(pickupTask);

        notifyOperatorsTaskCreated(savedTask, order);

        List<OrderDetail> details = orderDetailRepository.findByRentalOrder_OrderId(order.getOrderId());
        List<Device> allocatedDevices = deviceAllocationQueryService.getAllocatedDevicesForOrder(order.getOrderId());
        return RentalOrderResponseDto.from(order, details, allocatedDevices);
    }

    @Override
    public void delete(Long id) {
        if (!rentalOrderRepository.existsById(id)) {
            throw new NoSuchElementException("Không tìm thấy đơn thuê: " + id);
        }
        reservationService.cancelReservations(id);
        orderDetailRepository.deleteByRentalOrder_OrderId(id);
        rentalOrderRepository.deleteById(id);
    }

    private void notifyOperatorsOrderAndTaskCreated(RentalOrder order) {
        if (order == null || order.getOrderId() == null) {
            return;
        }
        List<Staff> operators = staffService.getStaffByRole(StaffRole.OPERATOR);
        if (operators == null || operators.isEmpty()) {
            return;
        }
        OperatorOrderNotification payload = OperatorOrderNotification.from(order);
        if (payload == null) {
            return;
        }
        operators.stream()
                .forEach(staff -> {
                    if (staff.getAccount() != null && staff.getAccount().getAccountId() != null) {
                        notificationService.notifyAccount(
                                staff.getAccount().getAccountId(),
                                NotificationType.ORDER_ACTIVE,
                                "Đơn hàng mới được tạo task",
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
                        log.warn("Không thể gửi thông báo đơn hàng {} tới operator {}: {}", order.getOrderId(), staffId, ex.getMessage());
                    }
                });
    }

    private void notifyOperatorsTaskCreated(Task task, RentalOrder order) {
        if (task == null || order == null || task.getTaskId() == null) {
            return;
        }
        List<Staff> operators = staffService.getStaffByRole(StaffRole.OPERATOR);
        if (operators == null || operators.isEmpty()) {
            return;
        }
        OperatorTaskNotification payload = OperatorTaskNotification.from(task, order);
        if (payload == null) {
            return;
        }
        operators.stream()
                .forEach(staff -> {
                    if (staff.getAccount() != null && staff.getAccount().getAccountId() != null) {
                        notificationService.notifyAccount(
                                staff.getAccount().getAccountId(),
                                NotificationType.ORDER_ACTIVE,
                                "Task mới cho đơn hàng",
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
                        log.warn("Không thể gửi thông báo task {} tới operator {}: {}", task.getTaskId(), staffId, ex.getMessage());
                    }
                });
    }

    private record OperatorOrderNotification(Long orderId,
                                             Long customerId,
                                             LocalDateTime startDate,
                                             LocalDateTime endDate,
                                             String shippingAddress,
                                             OrderStatus orderStatus,
                                             String message) {
        static OperatorOrderNotification from(RentalOrder order) {
            if (order == null) {
                return null;
            }
            String msg = "Đơn hàng #" + order.getOrderId() + " đã tạo task PRE_RENTAL_QC.";
            return new OperatorOrderNotification(
                    order.getOrderId(),
                    order.getCustomer() != null ? order.getCustomer().getCustomerId() : null,
                    order.getStartDate(),
                    order.getEndDate(),
                    order.getShippingAddress(),
                    order.getOrderStatus(),
                    msg
            );
        }
    }

    private record OperatorTaskNotification(Long taskId,
                                            Long orderId,
                                            Long taskCategoryId,
                                            String taskCategoryName,
                                            LocalDateTime plannedStart,
                                            LocalDateTime plannedEnd,
                                            String message) {
        static OperatorTaskNotification from(Task task, RentalOrder order) {
            if (task == null || order == null) {
                return null;
            }
            var category = task.getTaskCategory();
            String msg = "Tạo nhiệm vụ thu hồi đơn thuê #" + order.getOrderId() + " (task #" + task.getTaskId() + ").";
            return new OperatorTaskNotification(
                    task.getTaskId(),
                    order.getOrderId(),
                    category != null ? category.getTaskCategoryId() : null,
                    category != null ? category.getName() : null,
                    task.getPlannedStart(),
                    task.getPlannedEnd(),
                    msg
            );
        }
    }

    private Specification<RentalOrder> buildSpecification(String orderStatus, Long customerId, String shippingAddress, BigDecimal minTotalPrice, BigDecimal maxTotalPrice, BigDecimal minPricePerDay, BigDecimal maxPricePerDay, String startDateFrom, String startDateTo, String endDateFrom, String endDateTo, String createdAtFrom, String createdAtTo) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (orderStatus != null && !orderStatus.isBlank()) {
                try {
                    var st = OrderStatus.valueOf(orderStatus.toUpperCase());
                    predicate.getExpressions().add(cb.equal(root.get("orderStatus"), st));
                } catch (IllegalArgumentException ignored) {}
            }
            if (customerId != null) {
                predicate.getExpressions().add(cb.equal(root.join("customer").get("customerId"), customerId));
            }
            if (shippingAddress != null && !shippingAddress.isBlank()) {
                predicate.getExpressions().add(cb.like(cb.lower(root.get("shippingAddress")), "%" + shippingAddress.toLowerCase() + "%"));
            }
            if (minTotalPrice != null) {
                predicate.getExpressions().add(cb.greaterThanOrEqualTo(root.get("totalPrice"), minTotalPrice));
            }
            if (maxTotalPrice != null) {
                predicate.getExpressions().add(cb.lessThanOrEqualTo(root.get("totalPrice"), maxTotalPrice));
            }
            if (minPricePerDay != null) {
                predicate.getExpressions().add(cb.greaterThanOrEqualTo(root.get("pricePerDay"), minPricePerDay));
            }
            if (maxPricePerDay != null) {
                predicate.getExpressions().add(cb.lessThanOrEqualTo(root.get("pricePerDay"), maxPricePerDay));
            }
            LocalDateTime sFrom = parseDateTime(startDateFrom);
            if (sFrom != null) predicate.getExpressions().add(cb.greaterThanOrEqualTo(root.get("startDate"), sFrom));
            LocalDateTime sTo = parseDateTime(startDateTo);
            if (sTo != null) predicate.getExpressions().add(cb.lessThanOrEqualTo(root.get("startDate"), sTo));
            LocalDateTime eFrom = parseDateTime(endDateFrom);
            if (eFrom != null) predicate.getExpressions().add(cb.greaterThanOrEqualTo(root.get("endDate"), eFrom));
            LocalDateTime eTo = parseDateTime(endDateTo);
            if (eTo != null) predicate.getExpressions().add(cb.lessThanOrEqualTo(root.get("endDate"), eTo));
            LocalDateTime cFrom = parseDateTime(createdAtFrom);
            if (cFrom != null) predicate.getExpressions().add(cb.greaterThanOrEqualTo(root.get("createdAt"), cFrom));
            LocalDateTime cTo = parseDateTime(createdAtTo);
            if (cTo != null) predicate.getExpressions().add(cb.lessThanOrEqualTo(root.get("createdAt"), cTo));
            return predicate;
        };
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            try {
                return java.time.LocalDate.parse(value).atStartOfDay();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Computed computeFromDetails(RentalOrderRequestDto request) {
        List<OrderDetail> details = new ArrayList<>();
        BigDecimal totalPerDay = BigDecimal.ZERO;
        BigDecimal totalDeposit = BigDecimal.ZERO;

        if (request.getOrderDetails() == null || request.getOrderDetails().isEmpty()) {
            throw new IllegalArgumentException("Cần cung cấp orderDetails");
        }

        for (OrderDetailRequestDto d : request.getOrderDetails()) {
            if (d.getDeviceModelId() == null) {
                throw new IllegalArgumentException("Cần cung cấp deviceModelId trong OrderDetail");
            }
            DeviceModel model = deviceModelRepository.findById(d.getDeviceModelId())
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy DeviceModel: " + d.getDeviceModelId()));

            // Validate availability by BookingCalendar within requested time window
            if (d.getQuantity() == null) {
                throw new IllegalArgumentException("Cần cung cấp số lượng trong OrderDetail");
            }
            long available = bookingCalendarService.getAvailableCountByModel(
                    model.getDeviceModelId(), request.getStartDate(), request.getEndDate()
            );
            if (d.getQuantity() > available) {
                throw new IllegalArgumentException("Số lượng thuê vượt quá số thiết bị khả dụng trong thời gian đã chọn");
            }
//            /* Disabled old stock logic: using BookingCalendar availability
//            if (model.getAmountAvailable() == null || d.getQuantity() == null) {
//                throw new IllegalArgumentException("Số lượng thuê vượt quá số lượng trong kho");
//            }
//            if (d.getQuantity() > model.getAmountAvailable()) {
//                throw new IllegalArgumentException("Số lượng thuê vượt quá số lượng trong kho");
//            }
//            model.setAmountAvailable(model.getAmountAvailable() - d.getQuantity());
//            deviceModelRepository.save(model);
//            */

            BigDecimal linePerDay = model.getPricePerDay().multiply(BigDecimal.valueOf(d.getQuantity()));
            BigDecimal depositPerUnit = model.getDeviceValue().multiply(model.getDepositPercent());

            totalPerDay = totalPerDay.add(linePerDay);
            totalDeposit = totalDeposit.add(depositPerUnit.multiply(BigDecimal.valueOf(d.getQuantity())));

            OrderDetail detail = OrderDetail.builder()
                    .quantity(d.getQuantity())
                    .pricePerDay(linePerDay)
                    .depositAmountPerUnit(depositPerUnit)
                    .deviceModel(model)
                    .build();
            details.add(detail);
        }
        return new Computed(details, totalPerDay, totalDeposit);
    }

    private record Computed(List<OrderDetail> details, BigDecimal totalPerDay, BigDecimal totalDeposit) {}
}
