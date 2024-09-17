package com.halcyon.mediaservice.controller;

import com.halcyon.mediaservice.dto.CreateChildCommentDto;
import com.halcyon.mediaservice.dto.CreateCommentDto;
import com.halcyon.mediaservice.model.Comment;
import com.halcyon.mediaservice.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<Comment> create(@RequestBody @Valid CreateCommentDto dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        Comment comment = commentService.create(dto);
        return ResponseEntity.ok(comment);
    }

    @PostMapping("/by-parent")
    public ResponseEntity<Comment> createByParent(@RequestBody @Valid CreateChildCommentDto dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        Comment comment = commentService.create(dto);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> delete(@PathVariable long commentId) {
        String response = commentService.delete(commentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping( "/{commentId}")
    public ResponseEntity<Comment> getById(@PathVariable long commentId) {
        Comment comment = commentService.getById(commentId);
        return ResponseEntity.ok(comment);
    }

    @GetMapping
    public ResponseEntity<List<Comment>> getCommentsInPost(@RequestParam("postId") long postId) {
        List<Comment> comments = commentService.findAllByPost(postId);
        return ResponseEntity.ok(comments);
    }
}
