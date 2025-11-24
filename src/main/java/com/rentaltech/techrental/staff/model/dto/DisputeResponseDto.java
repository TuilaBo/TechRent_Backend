/* Dispute feature temporarily disabled
package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.DisputeOpenBy;
import com.rentaltech.techrental.staff.model.DisputeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResponseDto {
    private Long disputeId;
    private Long settlementId;
    private DisputeOpenBy openBy;
    private Long openedByCustomerId;  // null if opened by staff
    private Long openedByStaffId;     // null if opened by customer
    private String reason;
    private String detail;
    private DisputeStatus status;
    private LocalDateTime openAt;
    private LocalDateTime closedAt;
}
*/

