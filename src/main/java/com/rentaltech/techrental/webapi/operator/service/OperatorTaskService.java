package com.rentaltech.techrental.webapi.operator.service;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskResponseDto;

import java.util.List;

public interface OperatorTaskService {
    
    /**
     * Create task with assignment
     */
    Task createTask(TaskCreateRequestDto request);
    
    /**
     * Get task by ID
     */
    Task getTaskById(Long taskId);
    
    /**
     * Get tasks by order ID
     */
    List<Task> getTasksByOrder(Long orderId);
    
    /**
     * Get all tasks, optionally filter by assigned staff
     */
    List<Task> getTasks(Long assignedStaffId);
    
    /**
     * Map Task entity to TaskResponseDto
     */
    TaskResponseDto mapToResponseDto(Task task);
}

