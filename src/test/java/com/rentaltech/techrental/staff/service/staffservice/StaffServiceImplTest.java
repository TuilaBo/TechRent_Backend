package com.rentaltech.techrental.staff.service.staffservice;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.dto.AdminStaffCreateWithAccountRequestDto;
import com.rentaltech.techrental.staff.model.dto.StaffCreateRequestDto;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceImplTest {

    @Mock
    private StaffRepository staffRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private StaffServiceImpl staffService;

    @Test
    void searchStaffFiltersInactiveAndBusyStaff() {
        Staff available = staff(1L, true);
        Staff busy = staff(2L, true);
        Staff inactive = staff(3L, false);

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(2);

        when(staffRepository.findAll()).thenReturn(List.of(available, busy, inactive));
        when(taskRepository.existsOverlappingTaskForStaff(eq(1L), eq(start), eq(end))).thenReturn(false);
        when(taskRepository.existsOverlappingTaskForStaff(eq(2L), eq(start), eq(end))).thenReturn(true);

        List<Staff> result = staffService.searchStaff(start, end, true, null);

        assertThat(result).containsExactly(available);
        verify(taskRepository, times(1)).existsOverlappingTaskForStaff(eq(1L), eq(start), eq(end));
        verify(taskRepository, times(1)).existsOverlappingTaskForStaff(eq(2L), eq(start), eq(end));
    }

    @Test
    void searchStaffThrowsWhenAvailabilityRequestedWithoutPeriod() {
        assertThatThrownBy(() -> staffService.searchStaff(null, null, true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startTime");
    }

    @Test
    void searchStaffThrowsWhenEndBeforeStart() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.minusHours(1);

        assertThatThrownBy(() -> staffService.searchStaff(start, end, true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime");
    }

//    @Test
//    void getStaffCompletionStatsValidatesMonthRange() {
//        assertThatThrownBy(() -> staffService.getStaffCompletionStats(2024, 13, StaffRole.ADMIN, PageRequest.of(0, 10)))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("Tháng");
//        verify(taskRepository, never()).findStaffCompletionsByPeriod(
//                any(LocalDateTime.class),
//                any(LocalDateTime.class),
//                any(StaffRole.class),
//                any(Pageable.class));
//    }

    @Test
    void createStaffFailsWhenProfileAlreadyExists() {
        StaffCreateRequestDto request = StaffCreateRequestDto.builder()
                .accountId(10L)
                .staffRole(StaffRole.TECHNICIAN)
                .build();

        Account account = Account.builder().accountId(10L).build();
        when(accountService.getAccountById(10L)).thenReturn(Optional.of(account));
        when(staffRepository.findByAccount_AccountId(10L)).thenReturn(Optional.of(new Staff()));

        assertThatThrownBy(() -> staffService.createStaff(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Hồ sơ nhân viên đã tồn tại");
        verify(staffRepository, never()).save(any());
    }

    @Test
    void createStaffPersistsNewProfileWhenValid() {
        StaffCreateRequestDto request = StaffCreateRequestDto.builder()
                .accountId(20L)
                .staffRole(StaffRole.OPERATOR)
                .build();

        Account account = Account.builder().accountId(20L).build();
        when(accountService.getAccountById(20L)).thenReturn(Optional.of(account));
        when(staffRepository.findByAccount_AccountId(20L)).thenReturn(Optional.empty());
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Staff saved = staffService.createStaff(request);

        assertThat(saved.getAccount()).isEqualTo(account);
        assertThat(saved.getStaffRole()).isEqualTo(StaffRole.OPERATOR);
        verify(staffRepository).save(saved);
    }

    @Test
    void createStaffWithAccountMapsRole() {
        AdminStaffCreateWithAccountRequestDto request = AdminStaffCreateWithAccountRequestDto.builder()
                .username("user01")
                .email("user@example.com")
                .password("password")
                .phoneNumber("0900000000")
                .staffRole(StaffRole.ADMIN)
                .build();

        Account persistedAccount = Account.builder()
                .accountId(55L)
                .role(Role.ADMIN)
                .build();
        when(accountService.addAccount(any(Account.class))).thenReturn(persistedAccount);
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Staff saved = staffService.createStaffWithAccount(request);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountService).addAccount(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getAccount()).isEqualTo(persistedAccount);
        assertThat(saved.getStaffRole()).isEqualTo(StaffRole.ADMIN);
    }

    @Test
    void updateStaffRoleUpdatesAccountRole() {
        Account account = Account.builder()
                .accountId(70L)
                .role(Role.OPERATOR)
                .build();
        Staff staff = Staff.builder()
                .staffId(99L)
                .account(account)
                .staffRole(StaffRole.OPERATOR)
                .build();

        when(staffRepository.findById(99L)).thenReturn(Optional.of(staff));
        when(staffRepository.save(staff)).thenReturn(staff);

        Staff updated = staffService.updateStaffRole(99L, StaffRole.CUSTOMER_SUPPORT_STAFF);

        assertThat(updated.getStaffRole()).isEqualTo(StaffRole.CUSTOMER_SUPPORT_STAFF);
        assertThat(account.getRole()).isEqualTo(Role.CUSTOMER_SUPPORT_STAFF);
        verify(accountService).updateAccount(account);
        verify(staffRepository).save(staff);
    }

    private Staff staff(Long id, boolean active) {
        return Staff.builder()
                .staffId(id)
                .isActive(active)
                .staffRole(StaffRole.TECHNICIAN)
                .account(Account.builder()
                        .accountId(id)
                        .username("user-" + id)
                        .email("user" + id + "@example.com")
                        .password("secret")
                        .role(Role.TECHNICIAN)
                        .build())
                .build();
    }
}
