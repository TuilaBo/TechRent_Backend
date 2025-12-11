package com.rentaltech.techrental.staff.service.taskservice;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskCategoryType;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskUpdateRequestDto;

import java.time.LocalDate;
import java.util.List;

public interface TaskService {
    Task createTask(TaskCreateRequestDto request, String username);
    List<Task> getAllTasks();
    Task getTaskById(Long taskId);
    Task getTaskById(Long taskId, String username);
    List<Task> getTasksByCategory(TaskCategoryType category);
    List<Task> getTasksByOrder(Long orderId, String username);
    List<Task> getTasks(TaskCategoryType category, Long orderId, Long assignedStaffId, String status, String username);
    org.springframework.data.domain.Page<Task> getTasksWithPagination(TaskCategoryType category, Long orderId, Long assignedStaffId, String status, String username, org.springframework.data.domain.Pageable pageable);
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
                                                                                                     TaskCategoryType category);

    com.rentaltech.techrental.staff.model.dto.TaskRuleResponseDto getActiveTaskRule();

    List<com.rentaltech.techrental.staff.model.dto.StaffTaskCountByCategoryDto> getStaffTaskCountByCategory(Long staffId, LocalDate targetDate, TaskCategoryType category, String username);
}
