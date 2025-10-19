package com.rentaltech.techrental.staff.service.taskcategoryservice;

import com.rentaltech.techrental.staff.model.TaskCategory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface TaskCategoryService {
    List<TaskCategory> getAllTaskCategories();
    Optional<TaskCategory> getTaskCategoryById(Long categoryId);
    List<TaskCategory> searchTaskCategoriesByName(String name);
    boolean checkTaskCategoryExists(String name);
}
