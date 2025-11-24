package com.rentaltech.techrental.webapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Health Check", description = "Endpoint theo dõi trạng thái hệ thống")
public class HealthController {

    @GetMapping("/")
    @Operation(summary = "Root status", description = "Trả về thông tin tổng quan API")
    public ResponseEntity<?> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "TechRental API is running");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check chi tiết", description = "Trả về trạng thái hoạt động của dịch vụ")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "TechRental API");
        return ResponseEntity.ok(response);
    }
}





