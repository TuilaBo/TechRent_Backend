package com.rentaltech.techrental.staff.service.taskcategoryservice;

import com.rentaltech.techrental.staff.model.TaskCategoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class TaskCategoryServiceImplTest {

    private TaskCategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaskCategoryServiceImpl();
    }

    @Test
    void getAllTaskCategoriesReturnsAllEnumDefinitions() {
        List<Integer> ids = service.getAllTaskCategories().stream()
                .map(TaskCategoryType.TaskCategoryDefinition::taskCategoryId)
                .toList();
        assertThat(ids)
                .hasSize(TaskCategoryType.values().length)
                .containsExactlyInAnyOrderElementsOf(IntStream.rangeClosed(1, TaskCategoryType.values().length)
                        .boxed()
                        .toList());
    }

    @Test
    void getTaskCategoryFindsMatchingDefinition() {
        assertThat(service.getTaskCategory(TaskCategoryType.DELIVERY))
                .isPresent()
                .get()
                .extracting(TaskCategoryType.TaskCategoryDefinition::name)
                .isEqualTo(TaskCategoryType.DELIVERY.getName());
    }

    @Test
    void getTaskCategoryByIdReturnsDefinition() {
        int id = TaskCategoryType.DELIVERY.getId();
        assertThat(service.getTaskCategoryById(id))
                .isPresent()
                .get()
                .extracting(TaskCategoryType.TaskCategoryDefinition::taskCategoryId)
                .isEqualTo(id);
    }

    @Test
    void searchTaskCategoriesMatchesByNameOrDescription() {
        assertThat(service.searchTaskCategoriesByName("delivery"))
                .isNotEmpty()
                .first()
                .extracting(TaskCategoryType.TaskCategoryDefinition::taskCategoryId)
                .isEqualTo(TaskCategoryType.DELIVERY.getId());
    }

    @Test
    void checkTaskCategoryExistsValidatesDisplayName() {
        assertThat(service.checkTaskCategoryExists("Pre rental QC")).isTrue();
        assertThat(service.checkTaskCategoryExists("unknown")).isFalse();
    }
}
