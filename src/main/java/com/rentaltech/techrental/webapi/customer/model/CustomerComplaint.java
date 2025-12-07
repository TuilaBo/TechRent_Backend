package com.rentaltech.techrental.webapi.customer.model;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.Task;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_complaint")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerComplaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_id")
    private Long complaintId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder rentalOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private Allocation allocation; // Link với allocation hiện tại của device bị hỏng

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.PENDING;

    @Column(name = "customer_description", columnDefinition = "text", nullable = false)
    private String customerDescription; // Mô tả lỗi từ khách hàng

    @Column(name = "staff_note", columnDefinition = "text")
    private String staffNote; // Ghi chú từ staff khi xử lý

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_device_id")
    private Device replacementDevice; // Device thay thế (nếu có)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_task_id")
    private Task replacementTask; // Task đi đổi máy

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_allocation_id")
    private Allocation replacementAllocation; // Allocation mới cho device thay thế

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt; // Khi staff bắt đầu xử lý (tìm device thay thế)

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt; // Khi đã hoàn thành đổi máy

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_staff_id")
    private Staff resolvedBy; // Staff đã giải quyết

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ComplaintStatus.PENDING;
        }
    }
}

