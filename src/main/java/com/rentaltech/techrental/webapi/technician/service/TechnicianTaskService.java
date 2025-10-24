package com.rentaltech.techrental.webapi.technician.service;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.model.dto.TaskResponseDto;

import java.util.List;

public interface TechnicianTaskService {
    
    /**
     * Lấy tất cả task được assign cho technician
     */
    List<Task> getTasksForTechnician(Long technicianAccountId);
    
    /**
     * Lấy task theo ID (chỉ task được assign cho technician)
     */
    Task getTaskForTechnician(Long taskId, Long technicianAccountId);
    
    /**
     * Lấy task theo trạng thái cho technician
     */
    List<Task> getTasksByStatusForTechnician(TaskStatus status, Long technicianAccountId);
    
    /**
     * Lấy task theo order ID cho technician
     */
    List<Task> getTasksByOrderForTechnician(Long orderId, Long technicianAccountId);
    
    /**
     * Lấy task quá hạn cho technician
     */
    List<Task> getOverdueTasksForTechnician(Long technicianAccountId);
    
    /**
     * Cập nhật trạng thái task cho technician
     */
    Task updateTaskStatusForTechnician(Long taskId, TaskStatus status, Long technicianAccountId);
    
    /**
     * Map Task sang TaskResponseDto
     */
    TaskResponseDto mapToResponseDto(Task task);
}