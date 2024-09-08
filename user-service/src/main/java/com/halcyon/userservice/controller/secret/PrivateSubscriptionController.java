package com.halcyon.userservice.controller.secret;

import com.halcyon.userservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions/private")
@RequiredArgsConstructor
public class PrivateSubscriptionController {
    private final SubscriptionService subscriptionService;

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Integer>> getIdOfUsersSubscribedByUser(
            @PathVariable long ownerId,
            @RequestHeader("PrivateSecret") String privateSecret
    ) {
        List<Integer> emails = subscriptionService.getIdOfUsersSubscribedByUser(ownerId, privateSecret);
        return ResponseEntity.ok(emails);
    }
}
