package com.rentaltech.techrental.maintenance.repository;

import com.rentaltech.techrental.maintenance.model.MaintenanceSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, Long> {
    List<MaintenanceSchedule> findByDevice_DeviceId(Long deviceId);

    Page<MaintenanceSchedule> findByStartDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    List<MaintenanceSchedule> findByStartDateGreaterThanEqual(LocalDate startDate);
}


