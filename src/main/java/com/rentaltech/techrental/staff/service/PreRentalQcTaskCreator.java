package com.rentaltech.techrental.staff.service;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskCategoryType;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
@Component
@RequiredArgsConstructor
public class PreRentalQcTaskCreator {

    private final TaskRepository taskRepository;

    public void createIfNeeded(Long orderId) {
        if (orderId == null) {
            return;
        }
        boolean exists = taskRepository.findByOrderId(orderId).stream()
                .anyMatch(task -> task.getTaskCategory() == TaskCategoryType.PRE_RENTAL_QC);
        if (exists) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Task task = Task.builder()
                .taskCategory(TaskCategoryType.PRE_RENTAL_QC)
                .orderId(orderId)
                .description("Check Pre QC cho đơn thuê #" + orderId)
                .plannedStart(now)
                .plannedEnd(now.plusDays(3))
                .build();
        taskRepository.save(task);
    }
}
