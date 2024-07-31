package com.halcyon.clients.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "user-service",
        url = "http://localhost:8081"
)
public interface UserClient {
    @GetMapping("/api/v1/users/exists")
    boolean existsByEmail(@RequestParam("email") String email);

    @GetMapping("/api/v1/users/private")
    UserResponse getByEmail(@RequestParam("email") String email, @RequestHeader("PrivateSecret") String privateSecret);
}
