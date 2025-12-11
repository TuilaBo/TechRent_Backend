package com.rentaltech.techrental.staff.service.taskcategoryservice;

import com.rentaltech.techrental.staff.model.TaskCategoryType;

import java.util.List;
import java.util.Optional;

public interface TaskCategoryService {
    List<TaskCategoryType.TaskCategoryDefinition> getAllTaskCategories();

    Optional<TaskCategoryType.TaskCategoryDefinition> getTaskCategory(TaskCategoryType category);

    Optional<TaskCategoryType.TaskCategoryDefinition> getTaskCategoryById(int taskCategoryId);

    List<TaskCategoryType.TaskCategoryDefinition> searchTaskCategoriesByName(String name);

    boolean checkTaskCategoryExists(String name);
}
