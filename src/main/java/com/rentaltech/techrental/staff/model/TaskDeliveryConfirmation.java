package com.rentaltech.techrental.staff.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_delivery_confirmation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "staff_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDeliveryConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "confirmation_id")
    private Long confirmationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Staff staff;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;
}
