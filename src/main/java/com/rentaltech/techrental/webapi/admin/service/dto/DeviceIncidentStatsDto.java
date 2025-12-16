package com.rentaltech.techrental.webapi.admin.service.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DeviceIncidentStatsDto {
    int year;
    int month;
    long damageCount;
    long missingItemCount;
    long totalIncidents;
}


