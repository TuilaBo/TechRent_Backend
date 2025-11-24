/* Dispute feature temporarily disabled
package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    Optional<Dispute> findBySettlement_SettlementId(Long settlementId);
    List<Dispute> findAll();
}
*/

