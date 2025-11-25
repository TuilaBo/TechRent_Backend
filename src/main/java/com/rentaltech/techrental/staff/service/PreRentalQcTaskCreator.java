package com.rentaltech.techrental.staff.service;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
public class PreRentalQcTaskCreator {

    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;

    public void createIfNeeded(Long orderId) {
        if (orderId == null) {
            return;
        }
        boolean exists = taskRepository.findByOrderId(orderId).stream()
                .anyMatch(task -> "Pre rental QC".equalsIgnoreCase(task.getTaskCategory().getName()));
        if (exists) {
            return;
        }
        TaskCategory category = taskCategoryRepository.findByName("Pre rental QC")
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy TaskCategory 'Pre rental QC'"));
        LocalDateTime now = LocalDateTime.now();
        Task task = Task.builder()
                .taskCategory(category)
                .orderId(orderId)
                .description("Check Pre QC cho đơn thuê #" + orderId)
                .plannedStart(now)
                .plannedEnd(now.plusDays(3))
                .build();
        taskRepository.save(task);
    }
}
