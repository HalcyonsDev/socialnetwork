package com.halcyon.notificationservice.service;

import com.halcyon.notificationservice.payload.ForgotPasswordMessage;
import com.halcyon.notificationservice.payload.NewEmailVerificationMessage;
import com.halcyon.notificationservice.payload.UserIsBannedMessage;
import com.halcyon.notificationservice.payload.VerificationMessage;
import com.halcyon.notificationservice.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
    @Value("${spring.mail.username")
    private String fromEmail;

    private static final String AUTH_HOST = "http://localhost:8082";

    private final JavaMailSender mailSender;

    public void sendMailVerificationMessage(VerificationMessage verificationMessage) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setSubject("New User Account Verification");
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(verificationMessage.getTo());
        mailMessage.setText(EmailUtil.getEmailVerificationMessage(verificationMessage.getUsername(), AUTH_HOST, verificationMessage.getToken()));

        mailSender.send(mailMessage);
    }

    public void sendForgotPasswordMessage(ForgotPasswordMessage forgotPasswordMessage) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setSubject("Reset password");
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(forgotPasswordMessage.getEmail());
        mailMessage.setText(EmailUtil.getResetPasswordMessage(forgotPasswordMessage.getEmail(), AUTH_HOST,  forgotPasswordMessage.getToken()));

        mailSender.send(mailMessage);
    }

    public void sendNewEmailVerificationMessage(NewEmailVerificationMessage verificationMessage) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setSubject("New Email Verification Code");
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(verificationMessage.getTo());
        mailMessage.setText(String.valueOf(verificationMessage.getVerificationCode()));

        mailSender.send(mailMessage);
    }

    public void sendUserIsBannedMessage(UserIsBannedMessage userIsBannedMessage) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setSubject("You are banned.");
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(userIsBannedMessage.getBannedUserEmail());
        mailMessage.setText(EmailUtil.getUserIsBanendMessage(userIsBannedMessage.getUsername()));

        mailSender.send(mailMessage);
    }
}
