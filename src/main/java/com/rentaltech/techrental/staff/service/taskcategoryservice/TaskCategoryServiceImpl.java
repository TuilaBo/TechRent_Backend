package com.rentaltech.techrental.staff.service.taskcategoryservice;

import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.model.dto.TaskCategoryCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskCategoryUpdateRequestDto;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
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

    @Override
    public TaskCategory createTaskCategory(TaskCategoryCreateRequestDto request) {
        if (taskCategoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }

        TaskCategory category = TaskCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return taskCategoryRepository.save(category);
    }

    @Override
    public TaskCategory updateTaskCategory(Long categoryId, TaskCategoryUpdateRequestDto request) {
        TaskCategory category = taskCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NoSuchElementException("Danh mục với ID " + categoryId + " không tồn tại"));

        if (!category.getName().equals(request.getName()) && taskCategoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return taskCategoryRepository.save(category);
    }

    @Override
    public void deleteTaskCategory(Long categoryId) {
        if (!taskCategoryRepository.existsById(categoryId)) {
            throw new NoSuchElementException("Danh mục với ID " + categoryId + " không tồn tại");
        }
        taskCategoryRepository.deleteById(categoryId);
    }
}
