package com.halcyon.mediaservice.controller;

import com.halcyon.mediaservice.dto.CreateCommentByParentDto;
import com.halcyon.mediaservice.dto.CreateCommentDto;
import com.halcyon.mediaservice.model.Comment;
import com.halcyon.mediaservice.service.CommentService;
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
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Comment> create(@RequestBody @Valid CreateCommentDto dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        Comment comment = commentService.create(dto);
        return ResponseEntity.ok(comment);
    }

    @PostMapping(value = "/by-parent", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Comment> createByParent(@RequestBody @Valid CreateCommentByParentDto dto, BindingResult bindingResult) {
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

    @GetMapping(value = "/{commentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Comment> getById(@PathVariable long commentId) {
        Comment comment = commentService.findById(commentId);
        return ResponseEntity.ok(comment);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Comment>> getCommentsInPost(@RequestParam("postId") long postId) {
        List<Comment> comments = commentService.findAllByPost(postId);
        return ResponseEntity.ok(comments);
    }
}
