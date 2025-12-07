package com.rentaltech.techrental.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility helpers to convert query parameters into Pageable instances.
 */
public final class PageableUtil {

    private PageableUtil() {
    }

    public static Pageable buildPageRequest(int page, int size, List<String> sortParams) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 20;
        Sort sort = parseSort(sortParams);
        if (sort.isUnsorted()) {
            return PageRequest.of(safePage, safeSize);
        }
        return PageRequest.of(safePage, safeSize, sort);
    }

    private static Sort parseSort(List<String> sortParams) {
        if (sortParams == null || sortParams.isEmpty()) {
            return Sort.unsorted();
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (int i = 0; i < sortParams.size(); i++) {
            String param = sortParams.get(i);
            if (param == null || param.isBlank()) {
                continue;
            }
            String[] parts = param.split(",");
            String property = parts[0].trim();
            if (property.isEmpty()) {
                continue;
            }
            Sort.Direction direction = Sort.Direction.ASC;
            if (parts.length > 1) {
                direction = "desc".equalsIgnoreCase(parts[1].trim())
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;
            } else if (i + 1 < sortParams.size()) {
                String next = sortParams.get(i + 1);
                if (next != null && ("asc".equalsIgnoreCase(next.trim()) || "desc".equalsIgnoreCase(next.trim()))) {
                    direction = "desc".equalsIgnoreCase(next.trim())
                            ? Sort.Direction.DESC
                            : Sort.Direction.ASC;
                    i++;
                }
            }
            orders.add(new Sort.Order(direction, property));
        }
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }
}
