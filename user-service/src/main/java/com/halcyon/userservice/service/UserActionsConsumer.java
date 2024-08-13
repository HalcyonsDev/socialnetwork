package com.halcyon.userservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halcyon.userservice.dto.CreateUserDto;
import com.halcyon.userservice.dto.UserPasswordResetEvent;
import com.halcyon.userservice.payload.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserActionsConsumer {
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "createUser", groupId = "users")
    public void listenCreatingNews(String message) {
        CreateUserDto dto;

        try {
            dto = objectMapper.readValue(message, CreateUserDto.class);
            userService.create(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "resetPassword", groupId = "users")
    public void listenResetPassword(String message) {
        UserPasswordResetEvent passwordResetEvent;

        try {
            passwordResetEvent = objectMapper.readValue(message, UserPasswordResetEvent.class);
            userService.resetPassword(passwordResetEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "changeEmail", groupId = "users")
    public void listenChangeEmail(String message) {
        ChangeEmailMessage changeEmailMessage;

        try {
            changeEmailMessage = objectMapper.readValue(message, ChangeEmailMessage.class);
            userService.changeEmail(changeEmailMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "verify", groupId = "users")
    public void listenVerifyByEmail(String email) {
        userService.verifyByEmail(email);
    }

    @KafkaListener(topics = "saveSecret", groupId = "users")
    public void listenSaveSecret(String message) {
        SaveSecretMessage saveSecretMessage;

        try {
            saveSecretMessage = objectMapper.readValue(message, SaveSecretMessage.class);
            userService.saveSecret(saveSecretMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "use2FA", groupId = "users")
    public void listenUse2FA(String message) {
        Use2FAMessage use2FAMessage;

        try {
            use2FAMessage = objectMapper.readValue(message, Use2FAMessage.class);
            userService.use2FA(use2FAMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
