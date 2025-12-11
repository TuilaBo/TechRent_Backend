package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.TaskCategoryType;
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
    
    // Constructor cho JPQL query (không có taskCompletionsByCategory)
    public StaffTaskCompletionStatsDto(Long staffId, Long accountId, String username, 
                                      String email, String phoneNumber, StaffRole staffRole, 
                                      Long completedTaskCount) {
        this.staffId = staffId;
        this.accountId = accountId;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.staffRole = staffRole;
        this.completedTaskCount = completedTaskCount;
        this.taskCompletionsByCategory = null; // Sẽ được populate sau
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskCategoryCompletionDto {
        private TaskCategoryType taskCategory;
        private String taskCategoryDisplayName;
        private Long completedCount;
    }
}

