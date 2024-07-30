package com.halcyon.authservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halcyon.authservice.payload.ChangeEmailMessage;
import com.halcyon.authservice.payload.VerificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.halcyon.authservice.dto.RegisterUserDto;
import com.halcyon.authservice.payload.UserPasswordResetEvent;

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
            throw new RuntimeException(e);
        }
    }

    public void executeResetPassword(UserPasswordResetEvent passwordResetEvent) {
        try {
            String message = objectMapper.writeValueAsString(passwordResetEvent);
            kafkaTemplate.send("resetPassword", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeChangeEmail(ChangeEmailMessage changeEmailMessage) {
        try {
            String message = objectMapper.writeValueAsString(changeEmailMessage);
            kafkaTemplate.send("changeEmail", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeConfirmByEmail(String email) {
        kafkaTemplate.send("verify", email);
    }
}
