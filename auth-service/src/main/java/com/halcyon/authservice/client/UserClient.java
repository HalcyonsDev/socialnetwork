package com.halcyon.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import com.halcyon.authservice.payload.User;

@FeignClient(
        name = "user-service",
        url = "http://localhost:8081"
)
public interface UserClient {
    @GetMapping("/api/v1/users/exists")
    boolean existsByEmail(@RequestParam("email") String email);

    @GetMapping("/api/v1/users/private")
    User getByEmail(@RequestParam("email") String email, @RequestHeader("PrivateSecret") String privateSecret);
}