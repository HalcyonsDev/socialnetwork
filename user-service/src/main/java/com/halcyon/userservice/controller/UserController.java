package com.halcyon.userservice.controller;

import com.halcyon.userservice.model.User;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.halcyon.userservice.service.UserService;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsByEmail(@RequestParam("email") String email) {
        return ResponseEntity.ok(userService.existsByEmail(email));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getById(@PathVariable long userId) {
        User user = userService.findById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<User> getByToken(@RequestParam("token") String token) {
        User user = userService.findByToken(token);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/upload-avatar")
    public ResponseEntity<User> uploadAvatar(@RequestParam("avatar")MultipartFile imageFile) {
        User user = userService.uploadPhoto(imageFile);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/avatar/my")
    public ResponseEntity<File> getAvatar() {
        File avatar = userService.getAvatar();
        return ResponseEntity.ok(avatar);
    }

    @GetMapping("/avatar")
    public ResponseEntity<File> getAvatar(@RequestParam("email") String email) {
        File avatar = userService.getAvatar(email);
        return ResponseEntity.ok(avatar);
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
