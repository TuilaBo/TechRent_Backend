package com.rentaltech.techrental.webapi.technician.model.dto;

import com.rentaltech.techrental.webapi.technician.model.QCPhase;
import com.rentaltech.techrental.webapi.technician.model.QCResult;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QCReportUpdateRequestDto {
    @NotNull
    private QCPhase phase;

    @NotNull
    private QCResult result;

    private String findings;

    @Size(max = 500)
    private String accessorySnapShotUrl;

    private Map<Long, List<String>> orderDetailSerialNumbers;
}
