package com.rentaltech.techrental.staff.service.taskcategoryservice;

import com.rentaltech.techrental.staff.model.TaskCategoryType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class TaskCategoryServiceImpl implements TaskCategoryService {

    @Override
    public List<TaskCategoryType.TaskCategoryDefinition> getAllTaskCategories() {
        return TaskCategoryType.definitions();
    }

    @Override
    public Optional<TaskCategoryType.TaskCategoryDefinition> getTaskCategory(TaskCategoryType category) {
        if (category == null) {
            return Optional.empty();
        }
        return Optional.of(TaskCategoryType.toDefinition(category));
    }

    @Override
    public Optional<TaskCategoryType.TaskCategoryDefinition> getTaskCategoryById(int taskCategoryId) {
        return TaskCategoryType.fromId(taskCategoryId)
                .map(TaskCategoryType::toDefinition);
    }

    @Override
    public List<TaskCategoryType.TaskCategoryDefinition> searchTaskCategoriesByName(String name) {
        if (name == null || name.isBlank()) {
            return getAllTaskCategories();
        }
        String keyword = name.toLowerCase(Locale.ROOT);
        return TaskCategoryType.definitions().stream()
                .filter(def -> def.name().toLowerCase(Locale.ROOT).contains(keyword)
                        || (def.description() != null && def.description().toLowerCase(Locale.ROOT).contains(keyword)))
                .toList();
    }

    @Override
    public boolean checkTaskCategoryExists(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        try {
            TaskCategoryType.fromName(name);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
