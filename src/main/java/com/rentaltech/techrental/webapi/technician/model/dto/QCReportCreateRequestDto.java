package com.rentaltech.techrental.webapi.technician.model.dto;

import com.rentaltech.techrental.webapi.technician.model.QCPhase;
import com.rentaltech.techrental.webapi.technician.model.QCResult;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QCReportCreateRequestDto {
    @NotNull
    private Long taskId;

    @NotNull
    private Map<Long, List<String>> orderDetailSerialNumbers;

    @NotNull
    private QCPhase phase;

    @NotNull
    private QCResult result;

    private String findings;
}
