package com.halcyon.userservice.controller;

import com.halcyon.userservice.dto.RegisterOAuth2UserDto;
import com.halcyon.userservice.dto.UpdateOAuth2UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.service.UserService;

@RestController
@RequestMapping("/api/v1/users/private")
@RequiredArgsConstructor
public class PrivateUserController {
    private final UserService userService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> getByEmail(@RequestParam("email") String email) {
        User foundUser = userService.findByEmail(email);
        return ResponseEntity.ok(foundUser);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> registerOAuth2User(@RequestBody RegisterOAuth2UserDto dto) {
        User user = userService.registerOAuth2User(dto);
        return ResponseEntity.ok(user);
    }

    @PostMapping(value = "/update-data", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> updateOAuth2UserData(@RequestBody UpdateOAuth2UserDto dto) {
        User user = userService.updateOAuth2User(dto);
        return ResponseEntity.ok(user);
    }
}
