package com.rentaltech.techrental.staff.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Fixed set of task categories used across the platform.
 */
public enum TaskCategoryType {
    POST_RENTAL_QC("Post rental QC", "Kiểm tra chất lượng thiết bị sau khi khách trả."),
    MAINTENANCE("Maintenance", "Bảo trì thiết bị trong suốt vòng đời."),
    DELIVERY("Delivery", "Giao thiết bị đến địa chỉ khách hàng."),
    PICK_UP_RENTAL_ORDER("Pick up rental order", "Thu hồi thiết bị khi đơn thuê kết thúc."),
    PRE_RENTAL_QC("Pre rental QC", "Kiểm tra chất lượng thiết bị trước khi giao cho khách."),
    SETTLEMENT("Settlement", "Các nghiệp vụ chốt sổ, thanh toán và quyết toán.");

    private final String name;
    private final String description;

    TaskCategoryType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static TaskCategoryType fromName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Tên category không được để trống");
        }
        return Arrays.stream(values())
                .filter(type -> type.name.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy TaskCategory: " + value));
    }

    public int getId() {
        return this.ordinal() + 1;
    }

    public static List<TaskCategoryDefinition> definitions() {
        return Arrays.stream(values())
                .map(TaskCategoryType::toDefinition)
                .toList();
    }

    public static Optional<TaskCategoryType> fromId(int id) {
        if (id <= 0 || id > values().length) {
            return Optional.empty();
        }
        return Optional.of(values()[id - 1]);
    }

    public static TaskCategoryDefinition toDefinition(TaskCategoryType type) {
        if (type == null) {
            throw new IllegalArgumentException("TaskCategoryType không hợp lệ");
        }
        return new TaskCategoryDefinition(type.getId(), type.name, type.description);
    }

    public record TaskCategoryDefinition(int taskCategoryId, String name, String description) {}
}
