package com.rentaltech.techrental.staff.service.taskservice;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskUpdateRequestDto;

import java.util.List;

public interface TaskService {
    Task createTask(TaskCreateRequestDto request);
    List<Task> getAllTasks();
    Task getTaskById(Long taskId);
    List<Task> getTasksByCategory(Long categoryId);
    List<Task> getTasksByOrder(Long orderId);
    List<Task> getTasks(Long categoryId, Long orderId, Long assignedStaffId, String status);
    Task updateTask(Long taskId, TaskUpdateRequestDto request);
    void deleteTask(Long taskId);
    List<Task> getOverdueTasks();
}
