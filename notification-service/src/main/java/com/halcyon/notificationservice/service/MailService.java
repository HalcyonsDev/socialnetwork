package com.halcyon.notificationservice.service;

import com.halcyon.clients.subscribe.SubscriptionResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.notificationservice.payload.*;
import com.halcyon.notificationservice.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
    @Value("${spring.mail.username")
    private String fromEmail;

    @Value("${private.secret}")
    private String privateSecret;

    private static final String APP_HOST = "http://localhost:9191";

    private final JavaMailSender mailSender;
    private final UserClient userClient;

    public void sendMailVerificationMessage(VerificationMessage verificationMessage) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setSubject("New User Account Verification");
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(verificationMessage.getTo());
        mailMessage.setText(EmailUtil.getEmailVerificationMessage(verificationMessage.getUsername(), APP_HOST, verificationMessage.getToken()));

        mailSender.send(mailMessage);
    }

    public void sendForgotPasswordMessage(ForgotPasswordMessage forgotPasswordMessage) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setSubject("Reset password");
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(forgotPasswordMessage.getEmail());
        mailMessage.setText(EmailUtil.getResetPasswordMessage(forgotPasswordMessage.getEmail(), APP_HOST,  forgotPasswordMessage.getToken()));

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

    @Async
    public void sendNewPostMessage(NewPostMessage newPostMessage) {
        for (SubscriptionResponse subscriptionResponse: newPostMessage.getSubscribers()) {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setSubject("New Post.");
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(userClient.getPrivateById(subscriptionResponse.getOwner().getId(), privateSecret).getEmail());
            mailMessage.setText(EmailUtil.getNewPostMessage(
                    subscriptionResponse.getTarget().getUsername(),
                    subscriptionResponse.getOwner().getUsername(),
                    newPostMessage.getPostId(),
                    APP_HOST
            ));

            mailSender.send(mailMessage);
        }
    }
}
