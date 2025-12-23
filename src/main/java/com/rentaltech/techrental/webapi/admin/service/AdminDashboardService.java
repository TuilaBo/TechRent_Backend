package com.rentaltech.techrental.webapi.admin.service;

import com.rentaltech.techrental.webapi.admin.service.dto.*;

public interface AdminDashboardService {

    NewCustomerStatsDto getNewCustomers(int year, int month);

    DeviceImportByCategoryStatsDto getDeviceImportsByCategory(int year, int month);

    DeviceIncidentStatsDto getDeviceIncidents(int year, int month);

    DamageStatsDto getDamageStats(int year, int month);

    OrderStatusStatsDto getOrderStatusStats(int year, int month);
    
    RevenueStatsDto getRevenueStats(int year, Integer month, Integer day);
}


