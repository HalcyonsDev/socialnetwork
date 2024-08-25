package com.halcyon.authservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halcyon.authservice.exception.MessageSerializationException;
import com.halcyon.authservice.payload.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.halcyon.authservice.dto.RegisterUserDto;

@Service
@RequiredArgsConstructor
public class UserActionsProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void executeCreatingUser(RegisterUserDto dto) {
        try {
            String message = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("createUser", message);
        } catch (JsonProcessingException e) {
            throw new MessageSerializationException("Failed to serialize RegisterUserDto to JSON", e);
        }
    }

    public void executeResetPassword(UserPasswordResetMessage userPasswordResetMessage) {
        try {
            String message = objectMapper.writeValueAsString(userPasswordResetMessage);
            kafkaTemplate.send("resetPassword", message);
        } catch (JsonProcessingException e) {
            throw new MessageSerializationException("Failed to serialize UserPasswordResetMessage to JSON", e);
        }
    }

    public void executeChangeEmail(ChangeEmailMessage changeEmailMessage) {
        try {
            String message = objectMapper.writeValueAsString(changeEmailMessage);
            kafkaTemplate.send("changeEmail", message);
        } catch (JsonProcessingException e) {
            throw new MessageSerializationException("Failed to serialize ChangeEmailMessage to JSON", e);
        }
    }

    public void executeConfirmByEmail(String email) {
        kafkaTemplate.send("verify", email);
    }

    public void executeSaveSecret(SaveSecretMessage saveSecretMessage) {
        try {
            String message = objectMapper.writeValueAsString(saveSecretMessage);
            kafkaTemplate.send("saveSecret", message);
        } catch (JsonProcessingException e) {
            throw new MessageSerializationException("Failed to serialize SaveSecretMessage to JSON", e);
        }
    }

    public void executeUse2FA(String email) {
        kafkaTemplate.send("use2FA", email);
    }
}
