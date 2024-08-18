package com.halcyon.chatservice.controller;

import com.halcyon.chatservice.dto.CreateMessageDto;
import com.halcyon.chatservice.dto.UpdateMessageDto;
import com.halcyon.chatservice.model.Message;
import com.halcyon.chatservice.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @MessageMapping("/chat")
    public ResponseEntity<Message> create(@RequestBody @Valid CreateMessageDto dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        Message message = messageService.create(dto);
        return ResponseEntity.ok(message);
    }

    @PatchMapping("/update-content")
    public ResponseEntity<Message> updateContent(@RequestBody @Valid UpdateMessageDto dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        Message message = messageService.updateContent(dto);
        return ResponseEntity.ok(message);
    }

    @GetMapping(value = "/{messageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Message> getById(@PathVariable long messageId) {
        Message message = messageService.findById(messageId);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/count/{recipientId}")
    public ResponseEntity<Long> countNewMessages(@PathVariable long recipientId) {
        long newMessagesCount = messageService.countNewMessages(recipientId);
        return ResponseEntity.ok(newMessagesCount);
    }

    @GetMapping("/me/{recipientId}")
    public ResponseEntity<Page<Message>> getSentMeMessages(
            @PathVariable long recipientId,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "5") int limit
    ) {
        Page<Message> messages = messageService.findSentMeMessages(recipientId, offset, limit);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/my/{recipientId}")
    public ResponseEntity<Page<Message>> getMyMessages(
            @PathVariable long recipientId,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "5") int limit
    ) {
        Page<Message> messages = messageService.findMyMessages(recipientId, offset, limit);
        return ResponseEntity.ok(messages);
    }
}
