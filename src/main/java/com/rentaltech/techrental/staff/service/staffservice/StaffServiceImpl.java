package com.rentaltech.techrental.staff.service.staffservice;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.dto.AdminStaffCreateWithAccountRequestDto;
import com.rentaltech.techrental.staff.model.dto.StaffCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.StaffTaskCompletionStatsDto;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StaffServiceImpl implements StaffService {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TaskRepository taskRepository;

    @Override
    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    @Override
    public Optional<Staff> getStaffById(Long staffId) {
        return staffRepository.findById(staffId);
    }

    @Override
    public Staff getStaffByIdOrThrow(Long staffId) {
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên với id: " + staffId));
    }

    @Override
    public Optional<Staff> getStaffByAccountId(Long accountId) {
        return staffRepository.findByAccount_AccountId(accountId);
    }

    @Override
    public Staff getStaffByAccountIdOrThrow(Long accountId) {
        return staffRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên cho account id: " + accountId));
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
    public List<Staff> searchStaff(LocalDateTime startTime, LocalDateTime endTime, Boolean available, StaffRole staffRole) {
        List<Staff> base = (staffRole != null)
                ? staffRepository.findByStaffRole(staffRole)
                : staffRepository.findAll();

        List<Staff> activeStaff = base.stream()
                .filter(staff -> staff.getIsActive() == null || Boolean.TRUE.equals(staff.getIsActive()))
                .collect(Collectors.toList());

        boolean requireAvailability = available == null || available;
        if (!requireAvailability) {
            return activeStaff;
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Cần cung cấp startTime và endTime khi tìm nhân viên còn trống");
        }
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime phải sau startTime");
        }

        return activeStaff.stream()
                .filter(staff -> !taskRepository.existsOverlappingTaskForStaff(staff.getStaffId(), startTime, endTime))
                .collect(Collectors.toList());
    }

    @Override
    public org.springframework.data.domain.Page<StaffTaskCompletionStatsDto> getStaffCompletionStats(int year, int month, StaffRole staffRole, org.springframework.data.domain.Pageable pageable) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Tháng phải nằm trong khoảng 1-12");
        }
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = startDate.plusMonths(1).atStartOfDay().minusNanos(1);
        
        org.springframework.data.domain.Page<StaffTaskCompletionStatsDto> statsPage = taskRepository.findStaffCompletionsByPeriod(startTime, endTime, staffRole, pageable);
        
        // Lấy chi tiết task completion theo category cho từng staff
        List<StaffTaskCompletionStatsDto> stats = statsPage.getContent();
        for (StaffTaskCompletionStatsDto stat : stats) {
            List<Object[]> categoryRecords = taskRepository.countCompletedTasksByStaffAndCategory(
                    stat.getStaffId(), startTime, endTime);
            List<StaffTaskCompletionStatsDto.TaskCategoryCompletionDto> categoryCompletions = categoryRecords.stream()
                    .map(record -> StaffTaskCompletionStatsDto.TaskCategoryCompletionDto.builder()
                            .taskCategoryId((Long) record[0])
                            .taskCategoryName((String) record[1])
                            .completedCount((Long) record[2])
                            .build())
                    .toList();
            stat.setTaskCompletionsByCategory(categoryCompletions);
        }
        
        return new org.springframework.data.domain.PageImpl<>(stats, pageable, statsPage.getTotalElements());
    }

    @Override
    public Staff createStaff(StaffCreateRequestDto request) {
        // Kiểm tra Account có tồn tại không
        Account account = accountService.getAccountById(request.getAccountId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản với id: " + request.getAccountId()));

        // Kiểm tra Staff đã tồn tại chưa
        if (staffRepository.findByAccount_AccountId(request.getAccountId()).isPresent()) {
            throw new IllegalStateException("Hồ sơ nhân viên đã tồn tại cho tài khoản này");
        }

        Staff staff = Staff.builder()
                .account(account)
                .staffRole(request.getStaffRole())
                .build();

        return staffRepository.save(staff);
    }

    @Override
    @Transactional
    public Staff createStaffWithAccount(AdminStaffCreateWithAccountRequestDto request) {
        Account account = Account.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .phoneNumber(request.getPhoneNumber())
                .role(mapStaffRoleToAccountRole(request.getStaffRole()))
                .isActive(true)
                .build();
        Account saved = accountService.addAccount(account);

        Staff staff = Staff.builder()
                .account(saved)
                .staffRole(request.getStaffRole())
                .isActive(true)
                .build();
        return staffRepository.save(staff);
    }

    @Override
    public Staff updateStaffStatus(Long staffId, Boolean isActive) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên với id: " + staffId));
        staff.setIsActive(isActive);
        return staffRepository.save(staff);
    }

    @Override
    public Staff updateStaffRole(Long staffId, StaffRole staffRole) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên với id: " + staffId));
        
        // Cập nhật Staff Role
        staff.setStaffRole(staffRole);
        
        // Cập nhật Account Role tương ứng
        Account account = staff.getAccount();
        Role accountRole = mapStaffRoleToAccountRole(staffRole);
        account.setRole(accountRole);
        
        // Lưu cả Staff và Account
        accountService.updateAccount(account);
        return staffRepository.save(staff);
    }
    
    /**
     * Map StaffRole sang Account Role
     */
    private Role mapStaffRoleToAccountRole(StaffRole staffRole) {
        return switch (staffRole) {
            case ADMIN -> Role.ADMIN;
            case OPERATOR -> Role.OPERATOR;
            case TECHNICIAN -> Role.TECHNICIAN;
            case CUSTOMER_SUPPORT_STAFF -> Role.CUSTOMER_SUPPORT_STAFF;
        };
    }

    @Override
    public void deleteStaff(Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên với id: " + staffId));
        staff.setIsActive(false); // Soft delete
        staffRepository.save(staff);
    }
}
