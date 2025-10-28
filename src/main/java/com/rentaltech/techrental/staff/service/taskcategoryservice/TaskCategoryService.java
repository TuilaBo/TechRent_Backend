package com.rentaltech.techrental.staff.service.taskcategoryservice;

import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.model.dto.TaskCategoryCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskCategoryUpdateRequestDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface TaskCategoryService {
    List<TaskCategory> getAllTaskCategories();
    Optional<TaskCategory> getTaskCategoryById(Long categoryId);
    List<TaskCategory> searchTaskCategoriesByName(String name);
    boolean checkTaskCategoryExists(String name);

    TaskCategory createTaskCategory(TaskCategoryCreateRequestDto request);
    TaskCategory updateTaskCategory(Long categoryId, TaskCategoryUpdateRequestDto request);
    void deleteTaskCategory(Long categoryId);
}
