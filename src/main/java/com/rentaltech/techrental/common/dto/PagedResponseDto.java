package com.rentaltech.techrental.common.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PagedResponseDto {
    private List<?> content;
    private int page;
    private int size;
    private Long totalElements;
    private int totalPages;
    private boolean isLast;
    private int numberOfElements;
}

