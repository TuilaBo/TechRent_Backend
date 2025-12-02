package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    
    // Find by account
    Optional<Staff> findByAccount_AccountId(Long accountId);
    Optional<Staff> findByAccount_Username(String username);
    
    // Find by staff role
    List<Staff> findByStaffRole(StaffRole staffRole);
    
    // Find active staff
    List<Staff> findByIsActiveTrue();
    
    // Find active staff by role
    List<Staff> findByStaffRoleAndIsActiveTrue(StaffRole staffRole);
    

    


}
