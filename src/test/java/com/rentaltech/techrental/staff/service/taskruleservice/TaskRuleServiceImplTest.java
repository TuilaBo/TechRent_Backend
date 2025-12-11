package com.rentaltech.techrental.staff.service.taskruleservice;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.TaskCategoryType;
import com.rentaltech.techrental.staff.model.TaskRule;
import com.rentaltech.techrental.staff.model.dto.TaskRuleRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskRuleResponseDto;
import com.rentaltech.techrental.staff.repository.TaskRuleCustomRepository;
import com.rentaltech.techrental.staff.repository.TaskRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskRuleServiceImplTest {

    @Mock
    private TaskRuleRepository taskRuleRepository;
    @Mock
    private TaskRuleCustomRepository taskRuleCustomRepository;
    @Mock
    private AccountService accountService;
    private TaskRuleServiceImpl taskRuleService;

    private TaskRuleRequestDto baseRequest;

    @BeforeEach
    void setUp() {
        baseRequest = new TaskRuleRequestDto();
        baseRequest.setName("Max 5 tasks");
        baseRequest.setDescription("Limit per day");
        baseRequest.setMaxTasksPerDay(5);
        baseRequest.setActive(true);
        baseRequest.setEffectiveFrom(LocalDateTime.now().plusDays(1));
        baseRequest.setEffectiveTo(LocalDateTime.now().plusDays(10));
        baseRequest.setTaskCategory(TaskCategoryType.POST_RENTAL_QC);

        taskRuleService = new TaskRuleServiceImpl(taskRuleRepository, taskRuleCustomRepository, accountService);
    }

    @Test
    void createRejectsWhenEffectiveRangeInvalid() {
        baseRequest.setEffectiveFrom(LocalDateTime.now().plusDays(5));
        baseRequest.setEffectiveTo(LocalDateTime.now().plusDays(3));

        assertThatThrownBy(() -> taskRuleService.create(baseRequest, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectiveFrom");
        verify(taskRuleRepository, never()).save(any());
    }

    @Test
    void createRejectsWhenEffectiveToIsPast() {
        baseRequest.setEffectiveTo(LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> taskRuleService.create(baseRequest, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectiveTo");
        verify(taskRuleRepository, never()).save(any());
    }

    @Test
    void createPersistsRuleWithResolvedCreator() {
        Account account = Account.builder().username("rule-admin").build();
        when(accountService.getByUsername("rule-admin")).thenReturn(Optional.of(account));
        when(taskRuleRepository.save(any(TaskRule.class))).thenAnswer(invocation -> {
            TaskRule rule = invocation.getArgument(0);
            rule.setTaskRuleId(10L);
            return rule;
        });

        TaskRuleResponseDto response = taskRuleService.create(baseRequest, "rule-admin");

        assertThat(response.getTaskRuleId()).isEqualTo(10L);
        assertThat(response.getTaskCategory()).isEqualTo(TaskCategoryType.POST_RENTAL_QC);

        ArgumentCaptor<TaskRule> captor = ArgumentCaptor.forClass(TaskRule.class);
        verify(taskRuleRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("rule-admin");
        assertThat(captor.getValue().getTaskCategory()).isEqualTo(TaskCategoryType.POST_RENTAL_QC);
    }

    @Test
    void updateAppliesRequestAndCreator() {
        TaskRule existing = TaskRule.builder()
                .taskRuleId(20L)
                .name("Old name")
                .description("old")
                .taskCategory(TaskCategoryType.MAINTENANCE)
                .staffRole(StaffRole.OPERATOR)
                .build();

        TaskRuleRequestDto request = new TaskRuleRequestDto();
        request.setName("Updated");
        request.setDescription("new");
        request.setMaxTasksPerDay(7);
        request.setActive(false);
        request.setEffectiveFrom(LocalDateTime.now().plusDays(2));
        request.setEffectiveTo(LocalDateTime.now().plusDays(4));
        request.setTaskCategory(TaskCategoryType.DELIVERY);

        when(taskRuleRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(taskRuleRepository.save(existing)).thenReturn(existing);
        when(accountService.getByUsername("operator")).thenReturn(Optional.of(Account.builder().username("operator").build()));

        TaskRuleResponseDto response = taskRuleService.update(20L, request, "operator");

        assertThat(response.getName()).isEqualTo("Updated");
        assertThat(existing.getTaskCategory()).isEqualTo(TaskCategoryType.DELIVERY);
        assertThat(existing.getStaffRole()).isNull();
        assertThat(existing.getCreatedBy()).isEqualTo("operator");
    }

    @Test
    void deleteValidatesExistence() {
        when(taskRuleRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> taskRuleService.delete(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy");
    }

    @Test
    void getActiveRuleEntityDelegatesToCustomRepository() {
        TaskRule rule = TaskRule.builder().taskRuleId(30L).build();
        when(taskRuleCustomRepository.findActiveRuleByContext(any(LocalDateTime.class), eq(null), eq(null)))
                .thenReturn(Optional.of(rule));

        TaskRule active = taskRuleService.getActiveRuleEntity();

        assertThat(active).isEqualTo(rule);
    }
}
