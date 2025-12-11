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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskRuleServiceImpl implements TaskRuleService {

    private final TaskRuleRepository taskRuleRepository;
    private final TaskRuleCustomRepository taskRuleCustomRepository;
    private final AccountService accountService;

    @Override
    public TaskRuleResponseDto create(TaskRuleRequestDto request, String username) {
        validateEffectiveDates(request.getEffectiveFrom(), request.getEffectiveTo());
        TaskRule rule = mapToEntity(request);
        rule.setCreatedBy(resolveCreator(username));
        TaskRule saved = taskRuleRepository.save(rule);
        return TaskRuleResponseDto.from(saved);
    }

    @Override
    public TaskRuleResponseDto update(Long taskRuleId, TaskRuleRequestDto request, String username) {
        validateEffectiveDates(request.getEffectiveFrom(), request.getEffectiveTo());
        TaskRule existing = taskRuleRepository.findById(taskRuleId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy rule với id " + taskRuleId));
        applyRequest(existing, request);
        existing.setCreatedBy(resolveCreator(username));
        TaskRule saved = taskRuleRepository.save(existing);
        return TaskRuleResponseDto.from(saved);
    }

    @Override
    public void delete(Long taskRuleId) {
        if (!taskRuleRepository.existsById(taskRuleId)) {
            throw new NoSuchElementException("Không tìm thấy rule với id " + taskRuleId);
        }
        taskRuleRepository.deleteById(taskRuleId);
    }

    @Override
    public TaskRuleResponseDto get(Long taskRuleId) {
        TaskRule rule = taskRuleRepository.findById(taskRuleId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy rule với id " + taskRuleId));
        return TaskRuleResponseDto.from(rule);
    }

    @Override
    public List<TaskRuleResponseDto> list(Boolean active) {
        List<TaskRule> rules = active == null ? taskRuleRepository.findAll() : taskRuleRepository.findByActive(active);
        return rules.stream().map(TaskRuleResponseDto::from).toList();
    }

    @Override
    public List<TaskRuleResponseDto> getActiveRules() {
        List<TaskRule> rules = taskRuleCustomRepository.findAllActiveRules(java.time.LocalDateTime.now());
        return rules.stream().map(TaskRuleResponseDto::from).toList();
    }

    @Override
    public TaskRuleResponseDto getActiveRule() {
        return TaskRuleResponseDto.from(getActiveRuleEntity());
    }

    @Override
    public TaskRule getActiveRuleEntity() {
        return taskRuleCustomRepository
                .findActiveRuleByContext(java.time.LocalDateTime.now(), null, null)
                .orElse(null);
    }

    @Override
    public TaskRule getActiveRuleEntity(StaffRole role, TaskCategoryType taskCategory) {
        return taskRuleCustomRepository
                .findActiveRuleByContext(java.time.LocalDateTime.now(), role, taskCategory)
                .orElse(null);
    }

    @Override
    public TaskRule getActiveRuleEntityByCategory(TaskCategoryType taskCategory) {
        return taskRuleCustomRepository
                .findActiveRuleByContext(java.time.LocalDateTime.now(), null, taskCategory)
                .orElse(null);
    }

    private TaskRule mapToEntity(TaskRuleRequestDto request) {
        TaskRule rule = new TaskRule();
        applyRequest(rule, request);
        return rule;
    }

    private void applyRequest(TaskRule target, TaskRuleRequestDto request) {
        target.setName(request.getName());
        target.setDescription(request.getDescription());
        target.setMaxTasksPerDay(request.getMaxTasksPerDay());
        target.setActive(request.getActive());
        target.setEffectiveFrom(request.getEffectiveFrom());
        target.setEffectiveTo(request.getEffectiveTo());
        target.setStaffRole(null); // Không còn config staffRole nữa, luôn set null
        target.setTaskCategory(request.getTaskCategory());
    }

    private void validateEffectiveDates(java.time.LocalDateTime effectiveFrom, java.time.LocalDateTime effectiveTo) {
        if (effectiveFrom != null && effectiveTo != null) {
            if (effectiveFrom.isAfter(effectiveTo)) {
                throw new IllegalArgumentException("effectiveFrom không được sau effectiveTo");
            }
        }
        if (effectiveTo != null && effectiveTo.isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("effectiveTo không được ở quá khứ");
        }
    }

    private String resolveCreator(String username) {
        if (username == null) {
            return null;
        }
        Optional<Account> account = accountService.getByUsername(username);
        return account.map(Account::getUsername).orElse(username);
    }
}

