package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.OrderDetail;
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

    public RentalOrderServiceImpl(RentalOrderRepository rentalOrderRepository,
                                  OrderDetailRepository orderDetailRepository,
                                  CustomerRepository customerRepository,
                                  DeviceModelRepository deviceModelRepository) {
        this.rentalOrderRepository = rentalOrderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.customerRepository = customerRepository;
        this.deviceModelRepository = deviceModelRepository;
    }

    @Override
    public RentalOrderResponseDto create(RentalOrderRequestDto request) {
        RentalOrder order = mapToEntity(request);
        RentalOrder saved = rentalOrderRepository.save(order);

        List<OrderDetail> details = mapDetails(request.getOrderDetails(), saved);
        if (!details.isEmpty()) {
            orderDetailRepository.saveAll(details);
        }

        return mapToDto(saved, details);
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

        applyUpdates(existing, request);
        RentalOrder saved = rentalOrderRepository.save(existing);

        orderDetailRepository.deleteByRentalOrder_OrderId(id);
        List<OrderDetail> newDetails = mapDetails(request.getOrderDetails(), saved);
        if (!newDetails.isEmpty()) {
            orderDetailRepository.saveAll(newDetails);
        }

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

    private RentalOrder mapToEntity(RentalOrderRequestDto dto) {
        if (dto == null) throw new IllegalArgumentException("RentalOrderRequestDto is null");
        if (dto.getCustomerId() == null) throw new IllegalArgumentException("customerId is required");

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + dto.getCustomerId()));

        return RentalOrder.builder()
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .orderStatus(dto.getOrderStatus())
                .depositAmount(dto.getDepositAmount())
                .depositAmountHeld(dto.getDepositAmountHeld())
                .depositAmountUsed(dto.getDepositAmountUsed())
                .depositAmountRefunded(dto.getDepositAmountRefunded())
                .totalPrice(dto.getTotalPrice())
                .pricePerDay(dto.getPricePerDay())
                .customer(customer)
                .build();
    }

    private void applyUpdates(RentalOrder target, RentalOrderRequestDto dto) {
        if (dto == null) throw new IllegalArgumentException("RentalOrderRequestDto is null");

        if (dto.getCustomerId() == null) throw new IllegalArgumentException("customerId is required");
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + dto.getCustomerId()));

        target.setStartDate(dto.getStartDate());
        target.setEndDate(dto.getEndDate());
        target.setOrderStatus(dto.getOrderStatus());
        target.setDepositAmount(dto.getDepositAmount());
        target.setDepositAmountHeld(dto.getDepositAmountHeld());
        target.setDepositAmountUsed(dto.getDepositAmountUsed());
        target.setDepositAmountRefunded(dto.getDepositAmountRefunded());
        target.setTotalPrice(dto.getTotalPrice());
        target.setPricePerDay(dto.getPricePerDay());
        target.setCustomer(customer);
    }

    private List<OrderDetail> mapDetails(List<OrderDetailRequestDto> detailDtos, RentalOrder order) {
        List<OrderDetail> details = new ArrayList<>();
        if (detailDtos == null) return details;

        for (OrderDetailRequestDto d : detailDtos) {
            if (d.getDeviceModelId() == null) {
                throw new IllegalArgumentException("deviceModelId is required in OrderDetail");
            }
            DeviceModel model = deviceModelRepository.findById(d.getDeviceModelId())
                    .orElseThrow(() -> new NoSuchElementException("DeviceModel not found: " + d.getDeviceModelId()));

            OrderDetail detail = OrderDetail.builder()
                    .quantity(d.getQuantity())
                    .pricePerDay(d.getPricePerDay())
                    .depositAmountPerUnit(d.getDepositAmountPerUnit())
                    .rentalOrder(order)
                    .deviceModel(model)
                    .build();
            details.add(detail);
        }
        return details;
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
}
