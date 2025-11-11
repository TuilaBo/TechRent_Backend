package com.rentaltech.techrental.staff.service.staffservice;

import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.dto.StaffCreateRequestDto;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public interface StaffService {
    List<Staff> getAllStaff();
    Optional<Staff> getStaffById(Long staffId);
    Optional<Staff> getStaffByAccountId(Long accountId);
    Staff getStaffByIdOrThrow(Long staffId);
    Staff getStaffByAccountIdOrThrow(Long accountId);
    List<Staff> getStaffByRole(StaffRole staffRole);
    List<Staff> getActiveStaff();
    List<Staff> searchStaff(LocalDateTime startTime, LocalDateTime endTime, Boolean available, StaffRole staffRole);

    Staff createStaff(@Valid StaffCreateRequestDto request);
    Staff createStaffWithAccount(@jakarta.validation.Valid com.rentaltech.techrental.staff.model.dto.AdminStaffCreateWithAccountRequestDto request);
    Staff updateStaffStatus(Long staffId, Boolean isActive);
    Staff updateStaffRole(Long staffId, StaffRole staffRole);
    void deleteStaff(Long staffId);
}
