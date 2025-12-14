package com.rentaltech.techrental.staff.service.taskcategoryservice;

import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.model.dto.TaskCategoryCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskCategoryUpdateRequestDto;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskCategoryServiceImplTest {

    @Mock
    private TaskCategoryRepository taskCategoryRepository;

    @InjectMocks
    private TaskCategoryServiceImpl taskCategoryService;

    @Test
    void createTaskCategoryRejectsDuplicateName() {
        TaskCategoryCreateRequestDto request = TaskCategoryCreateRequestDto.builder()
                .name("Delivery")
                .description("Handle deliveries")
                .build();
        when(taskCategoryRepository.existsByName("Delivery")).thenReturn(true);

        assertThatThrownBy(() -> taskCategoryService.createTaskCategory(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã tồn tại");

        verify(taskCategoryRepository, never()).save(any());
    }

    @Test
    void createTaskCategoryPersistsNewRecord() {
        TaskCategoryCreateRequestDto request = TaskCategoryCreateRequestDto.builder()
                .name("Pickup")
                .description("Collect returned devices")
                .build();

        when(taskCategoryRepository.existsByName("Pickup")).thenReturn(false);
        when(taskCategoryRepository.save(any(TaskCategory.class))).thenAnswer(invocation -> {
            TaskCategory category = invocation.getArgument(0);
            category.setTaskCategoryId(5L);
            return category;
        });

        TaskCategory saved = taskCategoryService.createTaskCategory(request);

        assertThat(saved.getTaskCategoryId()).isEqualTo(5L);
        assertThat(saved.getName()).isEqualTo("Pickup");
        assertThat(saved.getDescription()).isEqualTo("Collect returned devices");
    }

    @Test
    void updateTaskCategoryValidatesRenameConflict() {
        TaskCategory existing = TaskCategory.builder()
                .taskCategoryId(7L)
                .name("Delivery")
                .description("Old")
                .build();

        TaskCategoryUpdateRequestDto request = TaskCategoryUpdateRequestDto.builder()
                .name("Support")
                .description("Customer support tasks")
                .build();

        when(taskCategoryRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(taskCategoryRepository.existsByName("Support")).thenReturn(true);

        assertThatThrownBy(() -> taskCategoryService.updateTaskCategory(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    void deleteTaskCategoryValidatesExistence() {
        when(taskCategoryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskCategoryService.deleteTaskCategory(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("không tồn tại");
    }
}
