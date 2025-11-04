package com.rentaltech.techrental.maintenance.service;

import com.rentaltech.techrental.maintenance.model.MaintenanceConflict;
import com.rentaltech.techrental.maintenance.model.MaintenanceConflictId;
import com.rentaltech.techrental.maintenance.model.MaintenanceConflictSource;
import com.rentaltech.techrental.maintenance.model.MaintenanceSchedule;
import com.rentaltech.techrental.maintenance.repository.MaintenanceConflictRepository;
import com.rentaltech.techrental.maintenance.repository.MaintenanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceConflictServiceImpl implements MaintenanceConflictService {

    private final MaintenanceConflictRepository conflictRepository;
    private final MaintenanceScheduleRepository scheduleRepository;

    @Override
    public MaintenanceConflict create(Long maintenanceScheduleId, Long bookingCalendarId, String status, MaintenanceConflictSource source) {
        MaintenanceSchedule schedule = scheduleRepository.findById(maintenanceScheduleId).orElseThrow();
        MaintenanceConflictId id = new MaintenanceConflictId(maintenanceScheduleId, bookingCalendarId);
        MaintenanceConflict conflict = MaintenanceConflict.builder()
                .id(id)
                .maintenanceSchedule(schedule)
                .bookingCalendarId(bookingCalendarId)
                .issuedAt(LocalDateTime.now())
                .status(status)
                .source(source)
                .build();
        return conflictRepository.save(conflict);
    }

    @Override
    public List<MaintenanceConflict> listBySchedule(Long maintenanceScheduleId) {
        return conflictRepository.findByMaintenanceSchedule_MaintenanceScheduleId(maintenanceScheduleId);
    }
}


