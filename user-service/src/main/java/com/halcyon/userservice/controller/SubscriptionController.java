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

    @DeleteMapping(value = "/unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestBody SubscriptionDto dto) {
        String response = subscriptionService.unsubscribe(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{subscriptionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Subscription> getById(@PathVariable long subscriptionId) {
        Subscription subscription = subscriptionService.getById(subscriptionId);
        return ResponseEntity.ok(subscription);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Subscription>> getSubscriptions() {
        List<Subscription> subscriptions = subscriptionService.findSubscriptions();
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping(value = "/owner/{ownerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Subscription>> getSubscriptionsByOwnerId(@PathVariable long ownerId){
        List<Subscription> subscriptions = subscriptionService.findSubscriptionsByOwnerId(ownerId);
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping(value = "/subscribers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Subscription>> getSubscribers() {
        List<Subscription> subscribers = subscriptionService.findSubscribers();
        return ResponseEntity.ok(subscribers);
    }

    @GetMapping(value = "/subscribers/target/{targetId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Subscription>> getSubscribersByTargetId(@PathVariable long targetId) {
        List<Subscription> subscribers = subscriptionService.findSubscribersByTargetId(targetId);
        return ResponseEntity.ok(subscribers);
    }
}
