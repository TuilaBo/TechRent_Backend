package com.rentaltech.techrental.contract.repository;

import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.model.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    
    Optional<Contract> findByContractNumber(String contractNumber);
    
    List<Contract> findByCustomerId(Long customerId);
    
    List<Contract> findByStatus(ContractStatus status);
    
    List<Contract> findByContractType(ContractType contractType);
    
    List<Contract> findByCreatedBy(Long createdBy);
    
    List<Contract> findByStatusAndCustomerId(ContractStatus status, Long customerId);

    Optional<Contract> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);
    
    @Query("SELECT c FROM Contract c WHERE c.status = :status AND c.expiresAt < :currentDate")
    List<Contract> findExpiredContracts(@Param("status") ContractStatus status, @Param("currentDate") LocalDateTime currentDate);
    
    @Query("SELECT c FROM Contract c WHERE c.status = :status AND c.expiresAt BETWEEN :startDate AND :endDate")
    List<Contract> findContractsExpiringSoon(@Param("status") ContractStatus status, 
                                            @Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(c) FROM Contract c WHERE c.customerId = :customerId AND c.status = :status")
    Long countByCustomerIdAndStatus(@Param("customerId") Long customerId, @Param("status") ContractStatus status);
    
    List<Contract> findByContractNumberStartingWith(String prefix);
    
    boolean existsByContractNumber(String contractNumber);
}
