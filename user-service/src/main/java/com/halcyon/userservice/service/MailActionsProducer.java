package com.halcyon.userservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halcyon.userservice.payload.UserIsBannedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailActionsProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void executeSendUserIsBannedMessage(UserIsBannedMessage userIsBannedMessage) {
        try {
            String message = objectMapper.writeValueAsString(userIsBannedMessage);
            kafkaTemplate.send("sendUserIsBannedMessage", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
