package com.rentaltech.techrental.staff.service.taskservice;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskUpdateRequestDto;

import java.time.LocalDate;
import java.util.List;

public interface TaskService {
    Task createTask(TaskCreateRequestDto request, String username);
    List<Task> getAllTasks();
    Task getTaskById(Long taskId);
    Task getTaskById(Long taskId, String username);
    List<Task> getTasksByCategory(Long categoryId);
    List<Task> getTasksByOrder(Long orderId, String username);
    List<Task> getTasks(Long categoryId, Long orderId, Long assignedStaffId, String status, String username);
    Task updateTask(Long taskId, TaskUpdateRequestDto request, String username);
    void deleteTask(Long taskId, String username);
    List<Task> getOverdueTasks();
    Task confirmDelivery(Long taskId, String username);
    Task confirmRetrieval(Long taskId, String username);

    List<com.rentaltech.techrental.staff.model.dto.StaffAssignmentDto> getStaffAssignmentsForDate(Long staffId,
                                                                                                  LocalDate targetDate,
                                                                                                  String username);

    List<com.rentaltech.techrental.staff.model.dto.TaskCompletionStatsDto> getMonthlyCompletionStats(int year,
                                                                                                     int month,
                                                                                                     Long categoryId);

    com.rentaltech.techrental.staff.model.dto.TaskRuleResponseDto getActiveTaskRule();
}
