package com.halcyon.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.halcyon.userservice.dto.CreateUserDto;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.service.UserService;

@RestController
@RequestMapping("/api/v1/users/private")
@RequiredArgsConstructor
public class PrivateUserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<User> getByEmail(@RequestParam("email") String email) {
        User foundUser = userService.findByEmail(email);
        return ResponseEntity.ok(foundUser);
    }
}
