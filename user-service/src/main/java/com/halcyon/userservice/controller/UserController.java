package com.halcyon.userservice.controller;

import com.halcyon.userservice.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import com.halcyon.userservice.service.UserService;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsByEmail(@RequestParam("email") String email) {
        return ResponseEntity.ok(userService.existsByEmail(email));
    }

    @PatchMapping(value = "/update-username", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> updateUsername(
            @Size(min = 1, max = 100, message = "Username must be more than 1 character and less than 100 characters.")
            @Pattern(regexp = "[a-zA-Z0-9-]+", message = "Username must contain only letters, digits, and dashes")
            @RequestParam("username") String username
    ) {
        User user = userService.updateUsername(username);
        return ResponseEntity.ok(user);
    }

    @PatchMapping(value = "/update-about", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> updateAbout(
            @Size(min = 1, max = 100, message = "\"About me\" must be more than 1 character and less than 500 characters.")
            @RequestParam("about") String about
    ) {
        User user = userService.updateAbout(about);
        return ResponseEntity.ok(user);
    }
}
