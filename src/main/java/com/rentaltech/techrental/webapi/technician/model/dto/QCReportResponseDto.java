package com.rentaltech.techrental.webapi.technician.model.dto;

import com.rentaltech.techrental.device.model.dto.DeviceResponseDto;
import com.rentaltech.techrental.webapi.technician.model.QCPhase;
import com.rentaltech.techrental.webapi.technician.model.QCResult;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QCReportResponseDto {
    private Long qcReportId;
    private QCPhase phase;
    private QCResult result;
    private String findings;
    private String accessorySnapShotUrl;
    private LocalDateTime createdAt;
    private String createdBy;
    private Long taskId;
    private Long orderDetailId;
    private Long orderId;
    private List<DeviceResponseDto> devices;
}
