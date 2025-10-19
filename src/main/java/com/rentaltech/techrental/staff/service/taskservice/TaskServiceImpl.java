package com.rentaltech.techrental.staff.service.taskservice;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskUpdateRequestDto;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.repository.TaskCustomRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCategoryRepository taskCategoryRepository;
    @Autowired
    private TaskCustomRepository taskCustomRepository;

    @Override
    public Task createTask(TaskCreateRequestDto request) {
        TaskCategory category = taskCategoryRepository.findById(request.getTaskCategoryId())
                .orElseThrow(() -> new RuntimeException("TaskCategory not found"));

        Task task = Task.builder()
                .taskCategory(category)
                .orderId(request.getOrderId())
                .type(request.getType())
                .description(request.getDescription())
                .plannedStart(request.getPlannedStart())
                .plannedEnd(request.getPlannedEnd())
                .build();

        return taskRepository.save(task);
    }

    @Override
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public Optional<Task> getTaskById(Long taskId) {
        return taskRepository.findById(taskId);
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
    public Task updateTask(Long taskId, TaskUpdateRequestDto request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (request.getTaskCategoryId() != null) {
            TaskCategory category = taskCategoryRepository.findById(request.getTaskCategoryId())
                    .orElseThrow(() -> new RuntimeException("TaskCategory not found"));
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
            throw new RuntimeException("Task not found");
        }
        taskRepository.deleteById(taskId);
    }

    @Override
    public List<Task> getOverdueTasks() {
        return taskCustomRepository.findOverdueTasks(LocalDateTime.now());
    }
}
