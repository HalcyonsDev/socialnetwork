package com.halcyon.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halcyon.notificationservice.payload.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailActionsConsumer {
    private final MailService mailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "sendVerificationMessage", groupId = "notifications")
    public void listenSendVerificationMessage(String message) {
        VerificationMessage verificationMessage;

        try {
            verificationMessage = objectMapper.readValue(message, VerificationMessage.class);
            mailService.sendMailVerificationMessage(verificationMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "sendForgotPasswordMessage", groupId = "notifications")
    public void listenSendForgotPasswordMessage(String message) {
        ForgotPasswordMessage forgotPasswordMessage;

        try {
            forgotPasswordMessage = objectMapper.readValue(message, ForgotPasswordMessage.class);
            mailService.sendForgotPasswordMessage(forgotPasswordMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "sendNewEmailVerificationMessage", groupId = "notifications")
    public void listenSendNewEmailVerificationMessage(String message) {
        NewEmailVerificationMessage verificationMessage;

        try {
            verificationMessage = objectMapper.readValue(message, NewEmailVerificationMessage.class);
            mailService.sendNewEmailVerificationMessage(verificationMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "sendUserIsBannedMessage", groupId = "notifications")
    public void listenSendUserIsBannedMessage(String message) {
        UserIsBannedMessage userIsBannedMessage;

        try {
            userIsBannedMessage = objectMapper.readValue(message, UserIsBannedMessage.class);
            mailService.sendUserIsBannedMessage(userIsBannedMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "sendNewPostMessage", groupId = "notifications")
    public void listenSendNewPostMessage(String message) {
        NewPostMessage newPostMessage;

        try {
            newPostMessage = objectMapper.readValue(message, NewPostMessage.class);
            mailService.sendNewPostMessage(newPostMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
