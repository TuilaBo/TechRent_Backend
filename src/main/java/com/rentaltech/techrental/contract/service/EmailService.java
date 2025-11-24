package com.rentaltech.techrental.contract.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Gửi Email OTP
     */
    public boolean sendOTP(String email, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("TechRental - Mã PIN ký hợp đồng");
            message.setText(buildOTPEmailContent(otp));
            
            mailSender.send(message);
            return true;
            
        } catch (Exception e) {
            System.err.println("Lỗi gửi email: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tạo nội dung email OTP
     */
    private String buildOTPEmailContent(String otp) {
        return String.format(
            "Xin chào,\n\n" +
            "Bạn đã yêu cầu ký hợp đồng trên hệ thống TechRental.\n\n" +
            "Mã PIN của bạn là: %s\n\n" +
            "Mã PIN có hiệu lực trong 5 phút.\n" +
            "Vui lòng không chia sẻ mã này với bất kỳ ai.\n\n" +
            "Nếu bạn không yêu cầu ký hợp đồng, vui lòng bỏ qua email này.\n\n" +
            "Trân trọng,\n" +
            "Đội ngũ TechRental",
            otp
        );
    }

    /**
     * Gửi email thông báo hợp đồng đã ký
     */
    public boolean sendContractSignedNotification(String email, String contractNumber) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("TechRental - Hợp đồng đã được ký thành công");
            message.setText(buildContractSignedContent(contractNumber));
            
            mailSender.send(message);
            return true;
            
        } catch (Exception e) {
            System.err.println("Lỗi gửi email thông báo: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gửi email nội dung tự do (sử dụng cho các nhắc nhở/notification)
     */
    public boolean sendGeneric(String email, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println("Lỗi gửi email: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tạo nội dung email thông báo hợp đồng đã ký
     */
    private String buildContractSignedContent(String contractNumber) {
        return String.format(
            "Xin chào,\n\n" +
            "Hợp đồng số %s đã được ký thành công.\n\n" +
            "Hợp đồng hiện đã có hiệu lực pháp lý.\n" +
            "Bạn có thể tải xuống bản sao hợp đồng từ hệ thống.\n\n" +
            "Cảm ơn bạn đã tin tưởng TechRental.\n\n" +
            "Trân trọng,\n" +
            "Đội ngũ TechRental",
            contractNumber
        );
    }
}

