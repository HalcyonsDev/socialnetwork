package com.halcyon.clients.subscribe;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "subscriptions",
        url = "http://localhost:8081"
)
public interface SubscribeClient {
    @GetMapping("/api/v1/subscriptions/subscribers/owner/{ownerId}")
    List<SubscriptionResponse> getSubscriptions(@PathVariable long ownerId);
}
