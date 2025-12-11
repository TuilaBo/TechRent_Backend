package com.rentaltech.techrental.staff.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_rule")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_rule_id")
    private Long taskRuleId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "max_tasks_per_day")
    private Integer maxTasksPerDay;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    /**
     * Rule áp dụng cho role cụ thể (TECHNICIAN, CUSTOMER_SUPPORT_STAFF, OPERATOR),
     * null = áp dụng cho mọi role.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "staff_role")
    private StaffRole staffRole;

    /**
     * Rule áp dụng cho TaskCategory cụ thể, null = áp dụng cho mọi category.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "task_category", length = 100)
    private TaskCategoryType taskCategory;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
