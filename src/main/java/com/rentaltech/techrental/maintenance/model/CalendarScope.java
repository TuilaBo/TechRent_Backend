package com.rentaltech.techrental.maintenance.model;

import java.util.Locale;

public enum CalendarScope {
    DAY,
    MONTH;

    public static CalendarScope from(String value) {
        if (value == null || value.isBlank()) {
            return DAY;
        }
        try {
            return CalendarScope.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("scope chỉ chấp nhận DAY hoặc MONTH");
        }
    }
}

