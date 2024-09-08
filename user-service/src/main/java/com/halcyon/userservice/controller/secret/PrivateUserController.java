package com.halcyon.userservice.controller.secret;

import com.halcyon.userservice.dto.RegisterOAuth2UserDto;
import com.halcyon.userservice.dto.UpdateOAuth2UserDto;
import com.halcyon.userservice.payload.PrivateUserResponse;
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
    public ResponseEntity<PrivateUserResponse> getByEmail(
            @RequestParam("email") String email,
            @RequestHeader("PrivateSecret") String privateSecret
    ) {
        User foundUser = userService.getByEmail(email, privateSecret);
        return ResponseEntity.ok(new PrivateUserResponse(foundUser));
    }

    @GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PrivateUserResponse> getById(
            @PathVariable long userId,
            @RequestHeader("PrivateSecret") String privateSecret
    ) {
        User foundUser = userService.getById(userId, privateSecret);
        return ResponseEntity.ok(new PrivateUserResponse(foundUser));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PrivateUserResponse> registerOAuth2User(
            @RequestBody RegisterOAuth2UserDto dto,
            @RequestHeader("PrivateSecret") String privateSecret
    ) {
        User user = userService.createOAuth2User(dto, privateSecret);
        return ResponseEntity.ok(new PrivateUserResponse(user));
    }

    @PostMapping(value = "/update-data", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PrivateUserResponse> updateOAuth2UserData(
            @RequestBody UpdateOAuth2UserDto dto,
            @RequestHeader("PrivateSecret") String privateSecret
    ) {
        User user = userService.updateOAuth2User(dto, privateSecret);
        return ResponseEntity.ok(new PrivateUserResponse(user));
    }
}
