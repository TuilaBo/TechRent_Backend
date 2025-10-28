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
        message.setSubject("Verify your email");
        message.setText("Your verification code is: " + code + "\nThis code expires in 10 minutes.");
        mailSender.send(message);
    }
}

