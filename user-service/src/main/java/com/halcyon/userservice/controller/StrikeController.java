package com.halcyon.userservice.controller;

import com.halcyon.userservice.dto.StrikeRequestDto;
import com.halcyon.userservice.model.Strike;
import com.halcyon.userservice.service.StrikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/strikes")
@RequiredArgsConstructor
public class StrikeController {
    private final StrikeService strikeService;

    @PostMapping(value = "/strike", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Strike> create(@RequestBody @Valid StrikeRequestDto dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        Strike strike = strikeService.create(dto);
        return ResponseEntity.ok(strike);
    }

    @GetMapping(value = "/sent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Strike>> getSentStrikes() {
        List<Strike> sentStrikes = strikeService.getSentStrikes();
        return ResponseEntity.ok(sentStrikes);
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Strike>> getSentMeStrikes() {
        List<Strike> sentMeStrikes = strikeService.getSentMeStrikes();
        return ResponseEntity.ok(sentMeStrikes);
    }
}
