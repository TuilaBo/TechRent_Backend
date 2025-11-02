package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, Long> {
    List<Allocation> findByOrderDetail_OrderDetailId(Long orderDetailId);

    List<Allocation> findByQcReport_QcReportId(Long qcReportId);
}

