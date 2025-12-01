package com.rentaltech.techrental.webapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Giám sát hệ thống", description = "Endpoint theo dõi trạng thái hoạt động của dịch vụ")
public class HealthController {

    @GetMapping("/")
    @Operation(summary = "Trạng thái gốc", description = "Trả về thông tin tổng quan API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "API hoạt động bình thường")
    })
    public ResponseEntity<?> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "TechRental API đang hoạt động ổn định");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check chi tiết", description = "Trả về trạng thái hoạt động của dịch vụ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dịch vụ đang hoạt động")
    })
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "TechRental API đang sẵn sàng");
        return ResponseEntity.ok(response);
    }
}





