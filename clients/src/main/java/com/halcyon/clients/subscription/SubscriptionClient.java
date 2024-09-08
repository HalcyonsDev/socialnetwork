package com.halcyon.clients.subscription;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(
        name = "subscriptions",
        url = "http://localhost:8081"
)
public interface SubscriptionClient {
    @GetMapping("/api/v1/subscriptions/subscribers/target/{targetId}")
    List<SubscriptionResponse> getSubscribers(@PathVariable long targetId);
    @GetMapping("/api/v1/subscriptions/private/owner/{ownerId}")
    List<Integer> getEmailsOfUsersSubscribedByUser(@PathVariable long ownerId, @RequestHeader("PrivateSecret") String privateSecret);
}
