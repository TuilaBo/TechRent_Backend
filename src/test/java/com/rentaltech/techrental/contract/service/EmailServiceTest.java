package com.rentaltech.techrental.contract.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendOtpReturnsTrueWhenMailSenderSucceeds() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertThat(emailService.sendOTP("user@test.com", "123456")).isTrue();
    }

    @Test
    void sendOtpReturnsFalseWhenMailSenderThrows() {
        doThrow(new RuntimeException("smtp"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThat(emailService.sendOTP("user@test.com", "123456")).isFalse();
    }

    @Test
    void sendContractSignedNotificationHandlesSuccessAndFailure() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        assertThat(emailService.sendContractSignedNotification("user@test.com", "HD01")).isTrue();

        doThrow(new RuntimeException("smtp"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        assertThat(emailService.sendContractSignedNotification("user@test.com", "HD01")).isFalse();
    }

    @Test
    void sendGenericReturnsFalseOnError() {
        doThrow(new RuntimeException("smtp")).when(mailSender).send(any(SimpleMailMessage.class));
        assertThat(emailService.sendGeneric("user@test.com", "Subject", "Body")).isFalse();
    }
}
