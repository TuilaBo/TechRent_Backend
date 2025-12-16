package com.rentaltech.techrental.webapi.admin.service;

import com.rentaltech.techrental.device.model.DiscrepancyType;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.device.repository.DiscrepancyReportRepository;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.staff.model.Settlement;
import com.rentaltech.techrental.staff.repository.SettlementRepository;
import com.rentaltech.techrental.webapi.admin.service.dto.*;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final CustomerRepository customerRepository;
    private final DeviceRepository deviceRepository;
    private final DiscrepancyReportRepository discrepancyReportRepository;
    private final SettlementRepository settlementRepository;
    private final RentalOrderRepository rentalOrderRepository;

    @Override
    public NewCustomerStatsDto getNewCustomers(int year, int month) {
        LocalDateTime start = firstDayOfMonth(year, month);
        LocalDateTime end = start.plusMonths(1);
        long count = customerRepository.countByCreatedAtBetween(start, end);
        return NewCustomerStatsDto.builder()
                .year(year)
                .month(month)
                .newCustomerCount(count)
                .build();
    }

    @Override
    public DeviceImportByCategoryStatsDto getDeviceImportsByCategory(int year, int month) {
        LocalDateTime start = firstDayOfMonth(year, month);
        LocalDateTime end = start.plusMonths(1);
        List<DeviceImportByCategoryStatsDto.CategoryCount> categories =
                deviceRepository.countImportsByCategoryBetween(start, end).stream()
                        .map(row -> DeviceImportByCategoryStatsDto.CategoryCount.builder()
                                .categoryId((Long) row[0])
                                .categoryName((String) row[1])
                                .deviceCount((Long) row[2])
                                .build())
                        .toList();
        return DeviceImportByCategoryStatsDto.builder()
                .year(year)
                .month(month)
                .categories(categories)
                .build();
    }

    @Override
    public DeviceIncidentStatsDto getDeviceIncidents(int year, int month) {
        LocalDateTime start = firstDayOfMonth(year, month);
        LocalDateTime end = start.plusMonths(1);
        long damage = discrepancyReportRepository
                .countByDiscrepancyTypeAndCreatedAtBetween(DiscrepancyType.DAMAGE, start, end);
        long missing = discrepancyReportRepository
                .countByDiscrepancyTypeAndCreatedAtBetween(DiscrepancyType.MISSING_ITEM, start, end);
        return DeviceIncidentStatsDto.builder()
                .year(year)
                .month(month)
                .damageCount(damage)
                .missingItemCount(missing)
                .totalIncidents(damage + missing)
                .build();
    }

    @Override
    public DamageStatsDto getDamageStats(int year, int month) {
        LocalDateTime start = firstDayOfMonth(year, month);
        LocalDateTime end = start.plusMonths(1);

        BigDecimal discrepancyTotal = discrepancyReportRepository.sumPenaltyAmountBetween(start, end);
        if (discrepancyTotal == null) discrepancyTotal = BigDecimal.ZERO;

        List<Settlement> settlements = settlementRepository.findByIssuedAtBetween(start, end);
        BigDecimal settlementDamageTotal = settlements.stream()
                .map(Settlement::getDamageFee)
                .filter(fee -> fee != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DamageStatsDto.builder()
                .year(year)
                .month(month)
                .discrepancyPenaltyTotal(discrepancyTotal)
                .settlementDamageFeeTotal(settlementDamageTotal)
                .totalDamage(discrepancyTotal.add(settlementDamageTotal))
                .build();
    }

    @Override
    public OrderStatusStatsDto getOrderStatusStats(int year, int month) {
        LocalDateTime start = firstDayOfMonth(year, month);
        LocalDateTime end = start.plusMonths(1);

        long completed = rentalOrderRepository.countByOrderStatusAndPlanEndDateBetween(
                OrderStatus.COMPLETED, start, end);
        long cancelled = rentalOrderRepository.countByOrderStatusAndCreatedAtBetween(
                OrderStatus.CANCELLED, start, end);

        return OrderStatusStatsDto.builder()
                .year(year)
                .month(month)
                .completedCount(completed)
                .cancelledCount(cancelled)
                .build();
    }

    private LocalDateTime firstDayOfMonth(int year, int month) {
        return LocalDate.of(year, month, 1).atStartOfDay();
    }
}


