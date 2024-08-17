package com.halcyon.clients.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "users",
        url = "http://localhost:8081"
)
public interface UserClient {
    @GetMapping("/api/v1/users/exists")
    boolean existsByEmail(@RequestParam("email") String email);

    @GetMapping("/api/v1/users/{userId}")
    UserResponse getById(@PathVariable long userId);

    @GetMapping("/api/v1/users/private")
    UserResponse getByEmail(@RequestParam("email") String email, @RequestHeader("PrivateSecret") String privateSecret);

    @PostMapping("/api/v1/users/private")
    UserResponse registerOAuth2User(@RequestBody RegisterOAuth2UserDto dto, @RequestHeader("PrivateSecret") String privateSecret);

    @PostMapping("/api/v1/users/private/update-data")
    UserResponse updateOAuth2UserData(@RequestBody UpdateOAuth2UserDto dto, @RequestHeader("PrivateSecret") String privateSecret);
}
