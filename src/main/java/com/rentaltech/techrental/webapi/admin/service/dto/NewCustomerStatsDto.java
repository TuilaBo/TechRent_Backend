package com.rentaltech.techrental.webapi.admin.service.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NewCustomerStatsDto {
    int year;
    int month;
    long newCustomerCount;
}


