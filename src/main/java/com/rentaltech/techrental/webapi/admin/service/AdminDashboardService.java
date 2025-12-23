package com.rentaltech.techrental.webapi.admin.service;

import com.rentaltech.techrental.webapi.admin.service.dto.DamageStatsDto;
import com.rentaltech.techrental.webapi.admin.service.dto.DeviceImportByCategoryStatsDto;
import com.rentaltech.techrental.webapi.admin.service.dto.DeviceIncidentStatsDto;
import com.rentaltech.techrental.webapi.admin.service.dto.NewCustomerStatsDto;
import com.rentaltech.techrental.webapi.admin.service.dto.OrderStatusStatsDto;
import com.rentaltech.techrental.webapi.admin.service.dto.RevenueStatsDto;

public interface AdminDashboardService {

    NewCustomerStatsDto getNewCustomers(int year, int month);

    DeviceImportByCategoryStatsDto getDeviceImportsByCategory(int year, int month);

    DeviceIncidentStatsDto getDeviceIncidents(int year, int month);

    DamageStatsDto getDamageStats(int year, int month);

    OrderStatusStatsDto getOrderStatusStats(int year, int month);

    /**
     * Thống kê doanh thu theo ngày/tháng/năm.
     * Doanh thu = tiền thuê (totalPrice) + tiền phạt + tiền bồi thường.
     * KHÔNG tính tiền cọc vào doanh thu.
     */
    RevenueStatsDto getRevenueStats(int year, Integer month, Integer day);
}


