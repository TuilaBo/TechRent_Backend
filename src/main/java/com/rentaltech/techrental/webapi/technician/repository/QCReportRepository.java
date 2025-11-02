package com.rentaltech.techrental.webapi.technician.repository;

import com.rentaltech.techrental.webapi.technician.model.QCReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface QCReportRepository extends JpaRepository<QCReport, Long> {
    Optional<QCReport> findByTask_TaskId(Long taskId);

    List<QCReport> findByTask_TaskIdIn(Collection<Long> taskIds);
}
