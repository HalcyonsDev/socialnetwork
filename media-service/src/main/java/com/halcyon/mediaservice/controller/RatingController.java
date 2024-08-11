package com.halcyon.mediaservice.controller;

import com.halcyon.mediaservice.dto.CreateRatingDto;
import com.halcyon.mediaservice.dto.UpdateRatingDto;
import com.halcyon.mediaservice.model.Rating;
import com.halcyon.mediaservice.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Rating> create(@RequestBody CreateRatingDto dto) {
        Rating rating = ratingService.create(dto);
        return ResponseEntity.ok(rating);
    }

    @GetMapping("/{ratingId}")
    public ResponseEntity<Rating> getById(@PathVariable long ratingId) {
        Rating rating = ratingService.findById(ratingId);
        return ResponseEntity.ok(rating);
    }

    @DeleteMapping("/{ratingId}")
    public ResponseEntity<String> delete(@PathVariable long ratingId) {
        String response = ratingService.delete(ratingId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/change-type")
    public ResponseEntity<Rating> changeType(@RequestBody UpdateRatingDto dto) {
        Rating rating = ratingService.changeType(dto);
        return ResponseEntity.ok(rating);
    }

    @GetMapping("/likes/{postId}")
    public ResponseEntity<Page<Rating>> getLikesInPost(
            @PathVariable long postId,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "5") int limit
    ) {
        Page<Rating> likes = ratingService.findLikesInPost(postId, offset, limit);
        return ResponseEntity.ok(likes);
    }

    @GetMapping("/dislikes/{postId}")
    public ResponseEntity<Page<Rating>> getDisLikesInPost(
            @PathVariable long postId,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "5") int limit
    ) {
        Page<Rating> likes = ratingService.findDislikesInPost(postId, offset, limit);
        return ResponseEntity.ok(likes);
    }
}
