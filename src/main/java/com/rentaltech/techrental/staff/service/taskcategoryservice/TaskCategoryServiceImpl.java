package com.rentaltech.techrental.staff.service.taskcategoryservice;

import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskCategoryServiceImpl implements TaskCategoryService {

    @Autowired
    private  TaskCategoryRepository taskCategoryRepository;

    @Override
    public List<TaskCategory> getAllTaskCategories() {
        return taskCategoryRepository.findAll();
    }

    @Override
    public Optional<TaskCategory> getTaskCategoryById(Long categoryId) {
        return taskCategoryRepository.findById(categoryId);
    }

    @Override
    public List<TaskCategory> searchTaskCategoriesByName(String name) {
        return taskCategoryRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public boolean checkTaskCategoryExists(String name) {
        return taskCategoryRepository.existsByName(name);
    }
}
