package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Query("""
            select case when count(t) > 0 then true else false end
            from Task t
            join t.assignedStaff s
            where s.staffId = :staffId
              and ( (t.plannedStart is null or t.plannedStart < :endTime)
                    and (t.plannedEnd is null or t.plannedEnd > :startTime) )
            """)
    boolean existsOverlappingTaskForStaff(@Param("staffId") Long staffId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
}
