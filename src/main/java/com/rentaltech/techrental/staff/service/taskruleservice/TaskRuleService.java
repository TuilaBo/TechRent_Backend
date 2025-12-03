package com.rentaltech.techrental.staff.service.taskruleservice;

import com.rentaltech.techrental.staff.model.TaskRule;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.dto.TaskRuleRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskRuleResponseDto;

import java.util.List;

public interface TaskRuleService {
    TaskRuleResponseDto create(TaskRuleRequestDto request, String username);

    TaskRuleResponseDto update(Long taskRuleId, TaskRuleRequestDto request, String username);

    void delete(Long taskRuleId);

    TaskRuleResponseDto get(Long taskRuleId);

    List<TaskRuleResponseDto> list(Boolean active);

    TaskRuleResponseDto getActiveRule();

    TaskRule getActiveRuleEntity();

    TaskRule getActiveRuleEntity(StaffRole role, Long taskCategoryId);
}

