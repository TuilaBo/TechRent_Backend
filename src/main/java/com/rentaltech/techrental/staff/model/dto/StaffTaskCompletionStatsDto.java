package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.StaffRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffTaskCompletionStatsDto {
    private Long staffId;
    private Long accountId;
    private String username;
    private String email;
    private String phoneNumber;
    private StaffRole staffRole;
    private Long completedTaskCount;
    private List<TaskCategoryCompletionDto> taskCompletionsByCategory;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskCategoryCompletionDto {
        private Long taskCategoryId;
        private String taskCategoryName;
        private Long completedCount;
    }
}

