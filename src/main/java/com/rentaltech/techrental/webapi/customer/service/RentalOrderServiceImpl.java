package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.OrderDetail;
import com.rentaltech.techrental.webapi.customer.model.OrderStatus;
import com.rentaltech.techrental.webapi.customer.model.RentalOrder;
import com.rentaltech.techrental.webapi.customer.model.dto.OrderDetailRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.OrderDetailResponseDto;
import com.rentaltech.techrental.webapi.customer.model.dto.RentalOrderRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.RentalOrderResponseDto;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.customer.repository.OrderDetailRepository;
import com.rentaltech.techrental.webapi.customer.repository.RentalOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class RentalOrderServiceImpl implements RentalOrderService {

    private final RentalOrderRepository rentalOrderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerRepository customerRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;

    public RentalOrderServiceImpl(RentalOrderRepository rentalOrderRepository,
                                  OrderDetailRepository orderDetailRepository,
                                  CustomerRepository customerRepository,
                                  DeviceModelRepository deviceModelRepository,
                                  TaskRepository taskRepository,
                                  TaskCategoryRepository taskCategoryRepository) {
        this.rentalOrderRepository = rentalOrderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.customerRepository = customerRepository;
        this.deviceModelRepository = deviceModelRepository;
        this.taskRepository = taskRepository;
        this.taskCategoryRepository = taskCategoryRepository;
    }

    @Override
    public RentalOrderResponseDto create(RentalOrderRequestDto request) {
        if (request == null) throw new IllegalArgumentException("RentalOrderRequestDto is null");
        if (request.getCustomerId() == null) throw new IllegalArgumentException("customerId is required");
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (days <= 0) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + request.getCustomerId()));

        // Build details from device models and compute totals
        Computed computed = computeFromDetails(request);

        RentalOrder order = RentalOrder.builder()
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .orderStatus(OrderStatus.PENDING)
                .depositAmount(computed.totalDeposit)
                .depositAmountHeld(0.0)
                .depositAmountUsed(0.0)
                .depositAmountRefunded(0.0)
                .totalPrice(computed.totalPerDay * days)
                .pricePerDay(computed.totalPerDay)
                .customer(customer)
                .build();

        RentalOrder saved = rentalOrderRepository.save(order);

        // attach order to details then save
        for (OrderDetail od : computed.details) od.setRentalOrder(saved);
        List<OrderDetail> persistedDetails = computed.details.isEmpty() ? List.of() : orderDetailRepository.saveAll(computed.details);

        // Create QC task linked to this order
        LocalDateTime now = LocalDateTime.now();
        TaskCategory category = taskCategoryRepository.findByName("Pre rental QC")
                .orElseThrow(() -> new NoSuchElementException("TaskCategory 'Pre rental QC' not found"));
        Task task = Task.builder()
                .taskCategory(category)
                .orderId(saved.getOrderId())
                .type("Pre rental QC")
                .plannedStart(now)
                .plannedEnd(now.plusDays(3))
                .build();
        taskRepository.save(task);


        return mapToDto(saved, persistedDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalOrderResponseDto findById(Long id) {
        RentalOrder order = rentalOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("RentalOrder not found: " + id));
        List<OrderDetail> details = orderDetailRepository.findByRentalOrder_OrderId(id);
        return mapToDto(order, details);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalOrderResponseDto> findAll() {
        List<RentalOrder> orders = rentalOrderRepository.findAll();
        List<RentalOrderResponseDto> result = new ArrayList<>(orders.size());
        for (RentalOrder order : orders) {
            List<OrderDetail> details = orderDetailRepository.findByRentalOrder_OrderId(order.getOrderId());
            result.add(mapToDto(order, details));
        }
        return result;
    }

    @Override
    public RentalOrderResponseDto update(Long id, RentalOrderRequestDto request) {
        RentalOrder existing = rentalOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("RentalOrder not found: " + id));

        if (request == null) throw new IllegalArgumentException("RentalOrderRequestDto is null");
        if (request.getCustomerId() == null) throw new IllegalArgumentException("customerId is required");
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (days <= 0) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + request.getCustomerId()));

        Computed computed = computeFromDetails(request);

        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        existing.setOrderStatus(OrderStatus.PENDING);
        existing.setDepositAmount(computed.totalDeposit);
        existing.setDepositAmountHeld(0.0);
        existing.setDepositAmountUsed(0.0);
        existing.setDepositAmountRefunded(0.0);
        existing.setTotalPrice(computed.totalPerDay * days);
        existing.setPricePerDay(computed.totalPerDay);
        existing.setCustomer(customer);

        RentalOrder saved = rentalOrderRepository.save(existing);

        orderDetailRepository.deleteByRentalOrder_OrderId(id);
        for (OrderDetail od : computed.details) od.setRentalOrder(saved);
        List<OrderDetail> newDetails = computed.details.isEmpty() ? List.of() : orderDetailRepository.saveAll(computed.details);

        return mapToDto(saved, newDetails);
    }

    @Override
    public void delete(Long id) {
        if (!rentalOrderRepository.existsById(id)) {
            throw new NoSuchElementException("RentalOrder not found: " + id);
        }
        orderDetailRepository.deleteByRentalOrder_OrderId(id);
        rentalOrderRepository.deleteById(id);
    }

    private Computed computeFromDetails(RentalOrderRequestDto request) {
        List<OrderDetail> details = new ArrayList<>();
        double totalPerDay = 0.0;
        double totalDeposit = 0.0;

        if (request.getOrderDetails() == null || request.getOrderDetails().isEmpty()) {
            throw new IllegalArgumentException("orderDetails is required");
        }

        for (OrderDetailRequestDto d : request.getOrderDetails()) {
            if (d.getDeviceModelId() == null) {
                throw new IllegalArgumentException("deviceModelId is required in OrderDetail");
            }
            DeviceModel model = deviceModelRepository.findById(d.getDeviceModelId())
                    .orElseThrow(() -> new NoSuchElementException("DeviceModel not found: " + d.getDeviceModelId()));

            double linePerDay = model.getPricePerDay() * d.getQuantity();
            double depositPerUnit = model.getDeviceValue() * model.getDepositPercent();

            totalPerDay += linePerDay;
            totalDeposit += depositPerUnit * d.getQuantity();

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

    private RentalOrderResponseDto mapToDto(RentalOrder order, List<OrderDetail> details) {
        List<OrderDetailResponseDto> detailDtos = new ArrayList<>();
        if (details != null) {
            for (OrderDetail d : details) {
                detailDtos.add(OrderDetailResponseDto.builder()
                        .orderDetailId(d.getOrderDetailId())
                        .quantity(d.getQuantity())
                        .pricePerDay(d.getPricePerDay())
                        .depositAmountPerUnit(d.getDepositAmountPerUnit())
                        .deviceModelId(d.getDeviceModel() != null ? d.getDeviceModel().getDeviceModelId() : null)
                        .build());
            }
        }

        return RentalOrderResponseDto.builder()
                .orderId(order.getOrderId())
                .startDate(order.getStartDate())
                .endDate(order.getEndDate())
                .orderStatus(order.getOrderStatus())
                .depositAmount(order.getDepositAmount())
                .depositAmountHeld(order.getDepositAmountHeld())
                .depositAmountUsed(order.getDepositAmountUsed())
                .depositAmountRefunded(order.getDepositAmountRefunded())
                .totalPrice(order.getTotalPrice())
                .pricePerDay(order.getPricePerDay())
                .createdAt(order.getCreatedAt())
                .customerId(order.getCustomer() != null ? order.getCustomer().getCustomerId() : null)
                .orderDetails(detailDtos)
                .build();
    }

    private record Computed(List<OrderDetail> details, double totalPerDay, double totalDeposit) {}
}
