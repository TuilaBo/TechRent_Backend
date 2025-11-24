package com.rentaltech.techrental.staff.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverReportItemRequestDto {

    @NotNull
    private Long deviceId;

    /**
     * Danh sách URL chứng cứ đã tồn tại (ví dụ đã upload lên Cloudinary trước đó).
     */
    @Builder.Default
    private List<String> evidenceUrls = new ArrayList<>();
}
