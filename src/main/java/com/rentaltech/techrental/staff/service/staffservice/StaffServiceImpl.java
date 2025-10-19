package com.rentaltech.techrental.staff.service.staffservice;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.dto.StaffCreateRequestDto;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StaffServiceImpl implements StaffService {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private AccountService accountService;

    @Override
    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    @Override
    public Optional<Staff> getStaffById(Long staffId) {
        return staffRepository.findById(staffId);
    }

    @Override
    public Optional<Staff> getStaffByAccountId(Long accountId) {
        return staffRepository.findByAccount_AccountId(accountId);
    }

    @Override
    public List<Staff> getStaffByRole(StaffRole staffRole) {
        return staffRepository.findByStaffRoleAndIsActiveTrue(staffRole);
    }

    @Override
    public List<Staff> getActiveStaff() {
        return staffRepository.findByIsActiveTrue();
    }

    @Override
    public Staff createStaff(StaffCreateRequestDto request) {
        // Kiểm tra Account có tồn tại không
        Account account = accountService.getAccountById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Kiểm tra Staff đã tồn tại chưa
        if (staffRepository.findByAccount_AccountId(request.getAccountId()).isPresent()) {
            throw new RuntimeException("Staff profile already exists for this account");
        }

        Staff staff = Staff.builder()
                .account(account)
                .staffRole(request.getStaffRole())
                .build();

        return staffRepository.save(staff);
    }

    @Override
    public Staff updateStaffStatus(Long staffId, Boolean isActive) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        staff.setIsActive(isActive);
        return staffRepository.save(staff);
    }

    @Override
    public Staff updateStaffRole(Long staffId, StaffRole staffRole) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        staff.setStaffRole(staffRole);
        return staffRepository.save(staff);
    }

    @Override
    public void deleteStaff(Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        staff.setIsActive(false); // Soft delete
        staffRepository.save(staff);
    }
}
