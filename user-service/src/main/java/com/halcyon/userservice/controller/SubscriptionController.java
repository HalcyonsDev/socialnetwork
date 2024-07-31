package com.halcyon.userservice.controller;

import com.halcyon.userservice.dto.SubscriptionDto;
import com.halcyon.userservice.model.Subscription;
import com.halcyon.userservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    @PostMapping(value = "/subscribe", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Subscription> subscribe(@RequestBody SubscriptionDto dto) {
        Subscription subscription = subscriptionService.subscribe(dto);
        return ResponseEntity.ok(subscription);
    }

    @DeleteMapping(value = "/unsubscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> unsubscribe(@RequestBody SubscriptionDto dto) {
        String response = subscriptionService.unsubscribe(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Subscription>> getSubscriptions() {
        List<Subscription> subscriptions = subscriptionService.getSubscriptions();
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping(value = "/subscribers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Subscription>> getSubscribers() {
        List<Subscription> subscribers = subscriptionService.getSubscribers();
        return ResponseEntity.ok(subscribers);
    }
}
