package com.rentaltech.techrental.contract.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

@Service
public class SMSService {

    @Value("${sms.provider.url:}")
    private String smsProviderUrl;

    @Value("${sms.provider.api-key:}")
    private String smsApiKey;

    @Value("${sms.provider.username:}")
    private String smsUsername;

    @Value("${sms.provider.password:}")
    private String smsPassword;

    private final WebClient webClient;

    public SMSService() {
        this.webClient = WebClient.builder().build();
    }

    /**
     * Gửi SMS OTP qua Viettel SMS API
     */
    public boolean sendOTP(String phoneNumber, String otp) {
        try {
            // Format số điện thoại Việt Nam
            String formattedPhone = formatVietnamesePhone(phoneNumber);
            
            // Nội dung SMS
            String message = "TechRental: Mã PIN để ký hợp đồng là: " + otp +
                           ". Mã có hiệu lực trong 5 phút. Không chia sẻ mã này với bất kỳ ai.";
            
            // Gọi API Viettel SMS
            return sendViettelSMS(formattedPhone, message);
            
        } catch (Exception e) {
            System.err.println("Lỗi gửi SMS: " + e.getMessage());
            return false;
        }
    }

    /**
     * Format số điện thoại Việt Nam
     */
    private String formatVietnamesePhone(String phoneNumber) {
        // Loại bỏ khoảng trắng và ký tự đặc biệt
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        
        // Thêm mã quốc gia nếu chưa có
        if (cleaned.startsWith("0")) {
            cleaned = "84" + cleaned.substring(1);
        } else if (!cleaned.startsWith("84")) {
            cleaned = "84" + cleaned;
        }
        
        return cleaned;
    }

    /**
     * Gửi SMS qua Viettel SMS API
     */
    private boolean sendViettelSMS(String phoneNumber, String message) {
        try {
            // Viettel SMS API endpoint
            String url = "https://api.viettelpost.vn/api/v1/sms/send";
            
            // Request body
            String requestBody = String.format(
                "{\n" +
                "  \"phone\": \"%s\",\n" +
                "  \"message\": \"%s\",\n" +
                "  \"brandname\": \"TechRental\"\n" +
                "}", phoneNumber, message
            );
            
            // Gửi request
            String response = webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + smsApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Kiểm tra response
            return response != null && response.contains("success");
            
        } catch (Exception e) {
            System.err.println("Lỗi gọi Viettel SMS API: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gửi SMS qua Twilio (alternative)
     */
    public boolean sendOTPViaTwilio(String phoneNumber, String otp) {
        try {
            String message = "TechRental: Mã PIN của bạn là: " + otp +
                           ". Mã có hiệu lực trong 5 phút. Không chia sẻ mã này.";
            
            // Twilio API implementation
            // Cần thêm Twilio SDK dependency
            return true; // Placeholder
            
        } catch (Exception e) {
            System.err.println("Lỗi gửi SMS qua Twilio: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gửi SMS qua local SMS gateway (for testing)
     */
    public boolean sendOTPViaLocalGateway(String phoneNumber, String otp) {
        try {
            // Mock implementation for testing
            System.out.println("=== SMS MOCK ===");
            System.out.println("To: " + phoneNumber);
            System.out.println("Message: TechRental: Mã PIN để ký hợp đồng là: " + otp);
            System.out.println("=================");
            
            // Simulate API call delay
            Thread.sleep(1000);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Lỗi mock SMS: " + e.getMessage());
            return false;
        }
    }
}

