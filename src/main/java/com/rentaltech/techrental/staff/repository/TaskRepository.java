package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalDate;
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
              and t.status in (com.rentaltech.techrental.staff.model.TaskStatus.PENDING,
                               com.rentaltech.techrental.staff.model.TaskStatus.IN_PROGRESS)
              and ( (t.plannedStart is null or t.plannedStart < :endTime)
                    and (t.plannedEnd is null or t.plannedEnd > :startTime) )
            """)
    boolean existsOverlappingTaskForStaff(@Param("staffId") Long staffId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    @Query("""
            select new com.rentaltech.techrental.staff.model.dto.StaffTaskCompletionStatsDto(
                    s.staffId,
                    s.account.accountId,
                    s.account.username,
                    s.account.email,
                    s.account.phoneNumber,
                    s.staffRole,
                    count(t)
            )
            from Task t
            join t.assignedStaff s
            where t.status = com.rentaltech.techrental.staff.model.TaskStatus.COMPLETED
              and t.completedAt between :startTime and :endTime
              and (:role is null or s.staffRole = :role)
            group by s.staffId, s.account.accountId, s.account.username, s.account.email, s.account.phoneNumber, s.staffRole
            order by count(t) desc
            """)
    List<com.rentaltech.techrental.staff.model.dto.StaffTaskCompletionStatsDto> findStaffCompletionsByPeriod(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("role") com.rentaltech.techrental.staff.model.StaffRole role);

    @Query("""
            select count(distinct t)
            from Task t
            join t.assignedStaff s
            where s.staffId = :staffId
              and t.status in (com.rentaltech.techrental.staff.model.TaskStatus.PENDING,
                               com.rentaltech.techrental.staff.model.TaskStatus.IN_PROGRESS)
              and (
                    (t.plannedStart is not null and function('DATE', t.plannedStart) = :targetDate)
                    or (t.plannedStart is null and function('DATE', t.createdAt) = :targetDate)
                  )
            """)
    long countActiveTasksByStaffAndDate(@Param("staffId") Long staffId,
                                        @Param("targetDate") LocalDate targetDate);

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
            select t.taskCategory.name, count(t)
            from Task t
            where t.status = com.rentaltech.techrental.staff.model.TaskStatus.COMPLETED
              and t.completedAt between :startTime and :endTime
              and (:categoryId is null or t.taskCategory.taskCategoryId = :categoryId)
            group by t.taskCategory.name
            order by t.taskCategory.name
            """)
    List<Object[]> countCompletedTasksByCategory(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime,
                                                 @Param("categoryId") Long categoryId);
}
