package com.rentaltech.techrental.webapi.customer.repository;

import com.rentaltech.techrental.webapi.customer.model.ComplaintStatus;
import com.rentaltech.techrental.webapi.customer.model.CustomerComplaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerComplaintRepository extends JpaRepository<CustomerComplaint, Long> {
    
    List<CustomerComplaint> findByRentalOrder_OrderId(Long orderId);
    
    List<CustomerComplaint> findByRentalOrder_OrderIdAndStatus(Long orderId, ComplaintStatus status);
    
    List<CustomerComplaint> findByStatus(ComplaintStatus status);
    
    List<CustomerComplaint> findByReplacementTask_TaskId(Long taskId);
}

