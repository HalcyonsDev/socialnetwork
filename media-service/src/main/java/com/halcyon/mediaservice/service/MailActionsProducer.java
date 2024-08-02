package com.halcyon.mediaservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halcyon.mediaservice.payload.NewPostMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailActionsProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void executeSendNewPostMessage(NewPostMessage newPostMessage) {
        try {
            String message = objectMapper.writeValueAsString(newPostMessage);
            kafkaTemplate.send("sendNewPostMessage", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
