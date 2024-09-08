package com.halcyon.mediaservice.controller;

import com.halcyon.mediaservice.dto.CreatePostDto;
import com.halcyon.mediaservice.dto.UpdatePostDto;
import com.halcyon.mediaservice.model.Post;
import com.halcyon.mediaservice.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @PostMapping
    public ResponseEntity<Post> create(@RequestBody @Valid CreatePostDto dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        Post post = postService.create(dto);
        return ResponseEntity.ok(post);
    }

    @GetMapping("/feed")
    public ResponseEntity<List<Post>> getFeed(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        List<Post> posts = postService.getFeedForUser(offset, limit);
        return ResponseEntity.ok(posts);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<String> delete(@PathVariable long postId) {
        String response = postService.delete(postId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<Post> getById(@PathVariable Long postId) {
        Post post = postService.getById(postId);
        return ResponseEntity.ok(post);
    }

    @PatchMapping(value = "/update")
    public ResponseEntity<Post> update(@RequestBody @Valid UpdatePostDto dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        Post post = postService.update(dto);
        return ResponseEntity.ok(post);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Post>> getMyPosts() {
        List<Post> posts = postService.getByPosts();
        return ResponseEntity.ok(posts);
    }

    @GetMapping
    public ResponseEntity<List<Post>> getUserPosts(@RequestParam("email") String email) {
        List<Post> posts = postService.getUserPosts(email);
        return ResponseEntity.ok(posts);
    }
}
