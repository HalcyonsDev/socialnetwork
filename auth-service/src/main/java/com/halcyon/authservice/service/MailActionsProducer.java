package com.halcyon.authservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halcyon.authservice.payload.ForgotPasswordMessage;
import com.halcyon.authservice.payload.NewEmailVerificationMessage;
import com.halcyon.authservice.payload.VerificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailActionsProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void executeSendVerificationMessage(VerificationMessage verificationMessage) {
        try {
            String message = objectMapper.writeValueAsString(verificationMessage);
            kafkaTemplate.send("sendVerificationMessage", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeSendForgotPasswordMessage(ForgotPasswordMessage forgotPasswordMessage) {
        try {
            String message = objectMapper.writeValueAsString(forgotPasswordMessage);
            kafkaTemplate.send("sendForgotPasswordMessage", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeSendNewEmailVerificationMessage(NewEmailVerificationMessage verificationMessage) {
        try {
            String message = objectMapper.writeValueAsString(verificationMessage);
            kafkaTemplate.send("sendNewEmailVerificationMessage", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
