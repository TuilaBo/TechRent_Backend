package com.rentaltech.techrental.policy.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "policy")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "file_url", length = 2048)
    private String fileUrl; // URL bản cho end-user xem (ưu tiên PDF)

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_type", length = 50)
    private String fileType; // PDF, DOC, DOCX

    @Column(name = "original_file_url", length = 2048)
    private String originalFileUrl; // URL file gốc (DOC/DOCX) cho admin tải về

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_by", length = 255)
    private String deletedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

