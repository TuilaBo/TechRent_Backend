package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskCategoryType;
import com.rentaltech.techrental.staff.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // Find tasks by category
    List<Task> findByTaskCategory(TaskCategoryType taskCategory);
    
    // Find tasks by order
    List<Task> findByOrderId(Long orderId);
    
    // Find tasks by status
    List<Task> findByStatus(TaskStatus status);
    
    // Find tasks by category and status
    List<Task> findByTaskCategoryAndStatus(TaskCategoryType taskCategory, TaskStatus status);

    @Query("""
            select case when count(t) > 0 then true else false end
            from Task t
            join t.assignedStaff s
            where s.staffId = :staffId
              and t.status in (com.rentaltech.techrental.staff.model.TaskStatus.PENDING,
                               com.rentaltech.techrental.staff.model.TaskStatus.IN_PROGRESS)
              and ( (t.plannedStart is null or t.plannedStart < :endTime)
                    and (t.plannedEnd is null or t.plannedEnd > :startTime) )
            """)
    boolean existsOverlappingTaskForStaff(@Param("staffId") Long staffId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);


    @Query("""
            select t
            from Task t
            join t.assignedStaff s
            where s.staffId = :staffId
              and (
                    (t.plannedStart is not null and function('DATE', t.plannedStart) = :targetDate)
                    or (t.plannedStart is null and function('DATE', t.createdAt) = :targetDate)
                  )
            order by t.plannedStart asc
            """)
    List<Task> findAssignmentsForStaffOnDate(@Param("staffId") Long staffId,
                                             @Param("targetDate") LocalDate targetDate);

    @Query("""
            select t.taskCategory, count(t)
            from Task t
            where t.status = com.rentaltech.techrental.staff.model.TaskStatus.COMPLETED
              and t.completedAt between :startTime and :endTime
              and (:category is null or t.taskCategory = :category)
            group by t.taskCategory
            order by t.taskCategory
            """)
    List<Object[]> countCompletedTasksByCategory(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime,
                                                 @Param("category") TaskCategoryType category);

    @Query("""
            select t.taskCategory, count(t)
            from Task t
            join t.assignedStaff s
            where t.status = com.rentaltech.techrental.staff.model.TaskStatus.COMPLETED
              and s.staffId = :staffId
              and t.completedAt between :startTime and :endTime
            group by t.taskCategory
            order by t.taskCategory
            """)
    List<Object[]> countCompletedTasksByStaffAndCategory(@Param("staffId") Long staffId,
                                                          @Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime);
}
