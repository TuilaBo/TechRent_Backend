package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.TaskDeliveryConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskDeliveryConfirmationRepository extends JpaRepository<TaskDeliveryConfirmation, Long> {

    Optional<TaskDeliveryConfirmation> findByTask_TaskIdAndStaff_StaffId(Long taskId, Long staffId);

    long countByTask_TaskIdAndStaff_StaffIdIn(Long taskId, Collection<Long> staffIds);

    List<TaskDeliveryConfirmation> findByTask_TaskId(Long taskId);

    void deleteByTask_TaskId(Long taskId);
}
