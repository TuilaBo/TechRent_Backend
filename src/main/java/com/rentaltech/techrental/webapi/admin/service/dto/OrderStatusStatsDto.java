package com.rentaltech.techrental.webapi.admin.service.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderStatusStatsDto {
    int year;
    int month;
    long completedCount;
    long cancelledCount;
}


