package com.halcyon.userservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halcyon.userservice.dto.CreateUserDto;
import com.halcyon.userservice.dto.UserPasswordResetEvent;
import com.halcyon.userservice.payload.ChangeEmailMessage;
import liquibase.change.Change;
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
}
