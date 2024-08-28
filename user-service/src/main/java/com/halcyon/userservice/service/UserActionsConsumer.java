package com.halcyon.userservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halcyon.userservice.dto.CreateUserDto;
import com.halcyon.userservice.dto.UserPasswordResetMessage;
import com.halcyon.userservice.exception.MessageDeserializationException;
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
    public void listenCreatingUser(String message) {
        CreateUserDto dto;

        try {
            dto = objectMapper.readValue(message, CreateUserDto.class);
            userService.create(dto);
        } catch (JsonProcessingException e) {
            throw new MessageDeserializationException("Failed to deserialize CreateUserDto to JSON", e);
        }
    }

    @KafkaListener(topics = "resetPassword", groupId = "users")
    public void listenResetPassword(String message) {
        UserPasswordResetMessage userPasswordResetMessage;

        try {
            userPasswordResetMessage = objectMapper.readValue(message, UserPasswordResetMessage.class);
            userService.resetPassword(userPasswordResetMessage);
        } catch (JsonProcessingException e) {
            throw new MessageDeserializationException("Failed to deserialize UserPasswordResetMessage to JSON", e);
        }
    }

    @KafkaListener(topics = "changeEmail", groupId = "users")
    public void listenChangeEmail(String message) {
        ChangeEmailMessage changeEmailMessage;

        try {
            changeEmailMessage = objectMapper.readValue(message, ChangeEmailMessage.class);
            userService.changeEmail(changeEmailMessage);
        } catch (JsonProcessingException e) {
            throw new MessageDeserializationException("Failed to deserialize ChangeEmailMessage to JSON", e);
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
            throw new MessageDeserializationException("Failed to deserialize SaveSecretMessage to JSON", e);
        }
    }

    @KafkaListener(topics = "use2FA", groupId = "users")
    public void listenUse2FA(String email) {
        userService.use2FA(email);
    }
}
