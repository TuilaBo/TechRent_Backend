package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // Find tasks by category
    List<Task> findByTaskCategory_TaskCategoryId(Long taskCategoryId);
    
    // Find tasks by order
    List<Task> findByOrderId(Long orderId);
    
    // Find tasks by status
    List<Task> findByStatus(TaskStatus status);
    
    // Find tasks by category and status
    List<Task> findByTaskCategory_TaskCategoryIdAndStatus(Long taskCategoryId, TaskStatus status);

}
