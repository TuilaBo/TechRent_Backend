package com.rentaltech.techrental.webapi.admin.service.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DeviceImportByCategoryStatsDto {
    int year;
    int month;
    List<CategoryCount> categories;

    @Value
    @Builder
    public static class CategoryCount {
        Long categoryId;
        String categoryName;
        long deviceCount;
    }
}


