package com.rentaltech.techrental.authentication.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class VerificationEmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendVerificationEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Xác thực email của bạn");
        message.setText("Mã xác thực của bạn là: " + code + "\nMã sẽ hết hạn sau 10 phút.");
        mailSender.send(message);
    }

    @Async
    public void sendResetPasswordEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Mã đặt lại mật khẩu");
        message.setText("Mã đặt lại mật khẩu của bạn là: " + code + "\nMã sẽ hết hạn sau 10 phút.\n\nNếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.");
        mailSender.send(message);
    }
}

