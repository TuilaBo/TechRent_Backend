package com.rentaltech.techrental.staff.service.taskservice;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskUpdateRequestDto;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.repository.TaskCustomRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import com.rentaltech.techrental.common.exception.TaskNotFoundException;
import com.rentaltech.techrental.common.exception.TaskCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCategoryRepository taskCategoryRepository;
    
    @Autowired
    private TaskCustomRepository taskCustomRepository;
    
    @Autowired
    private StaffRepository staffRepository;

    @Override
    public Task createTask(TaskCreateRequestDto request) {
        try {
            // Validate business rules
            validateTaskCreationRequest(request);
            
            TaskCategory category = taskCategoryRepository.findById(request.getTaskCategoryId())
                    .orElseThrow(() -> new NoSuchElementException("TaskCategory not found"));

            // Tìm staff được assign (nếu có)
            Staff assignedStaff = null;
            if (request.getAssignedStaffId() != null) {
                assignedStaff = staffRepository.findById(request.getAssignedStaffId())
                        .orElseThrow(() -> new NoSuchElementException("Staff not found"));
            }

            Task task = Task.builder()
                    .taskCategory(category)
                    .orderId(request.getOrderId())
                    .assignedStaff(assignedStaff)
                    .type(request.getType())
                    .description(request.getDescription())
                    .plannedStart(request.getPlannedStart())
                    .plannedEnd(request.getPlannedEnd())
                    .build();

            return taskRepository.save(task);
            
        } catch (IllegalArgumentException e) {
            throw new TaskCreationException("Validation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new TaskCreationException("Failed to create task: " + e.getMessage(), e);
        }
    }
    
    private void validateTaskCreationRequest(TaskCreateRequestDto request) {
        if (request.getTaskCategoryId() == null) {
            throw new IllegalArgumentException("TaskCategoryId is required");
        }
        if (request.getOrderId() == null) {
            throw new IllegalArgumentException("OrderId is required");
        }
        if (request.getPlannedStart() != null && request.getPlannedEnd() != null) {
            if (request.getPlannedStart().isAfter(request.getPlannedEnd())) {
                throw new IllegalArgumentException("Planned start time cannot be after planned end time");
            }
        }
    }

    @Override
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public Task getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with ID " + taskId + " not found"));
    }

    @Override
    public List<Task> getTasksByCategory(Long categoryId) {
        return taskRepository.findByTaskCategory_TaskCategoryId(categoryId);
    }

    @Override
    public List<Task> getTasksByOrder(Long orderId) {
        return taskRepository.findByOrderId(orderId);
    }

    @Override
    public List<Task> getTasks(Long categoryId, Long orderId, Long assignedStaffId, String status) {
        List<Task> tasks = taskRepository.findAll();
        if (categoryId != null) {
            tasks = tasks.stream()
                    .filter(task -> task.getTaskCategory().getTaskCategoryId().equals(categoryId))
                    .collect(Collectors.toList());
        }
        if (orderId != null) {
            tasks = tasks.stream()
                    .filter(task -> task.getOrderId().equals(orderId))
                    .collect(Collectors.toList());
        }
        if (assignedStaffId != null) {
            tasks = tasks.stream()
                    .filter(task -> task.getAssignedStaff() != null &&
                            task.getAssignedStaff().getStaffId().equals(assignedStaffId))
                    .collect(Collectors.toList());
        }
        if (status != null) {
            tasks = tasks.stream()
                    .filter(task -> task.getStatus().toString().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }
        return tasks;
    }

    @Override
    public Task updateTask(Long taskId, TaskUpdateRequestDto request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));

        if (request.getTaskCategoryId() != null) {
            TaskCategory category = taskCategoryRepository.findById(request.getTaskCategoryId())
                    .orElseThrow(() -> new NoSuchElementException("TaskCategory not found"));
            task.setTaskCategory(category);
        }
        if (request.getType() != null) task.setType(request.getType());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPlannedStart() != null) task.setPlannedStart(request.getPlannedStart());
        if (request.getPlannedEnd() != null) task.setPlannedEnd(request.getPlannedEnd());
        if (request.getStatus() != null) task.setStatus(request.getStatus());

        return taskRepository.save(task);
    }

    @Override
    public void deleteTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new NoSuchElementException("Task not found");
        }
        taskRepository.deleteById(taskId);
    }

    @Override
    public List<Task> getOverdueTasks() {
        return taskCustomRepository.findOverdueTasks(LocalDateTime.now());
    }
}
