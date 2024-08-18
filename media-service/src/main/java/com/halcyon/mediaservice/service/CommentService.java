package com.halcyon.mediaservice.service;

import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.mediaservice.dto.CreateCommentByParentDto;
import com.halcyon.mediaservice.dto.CreateCommentDto;
import com.halcyon.mediaservice.exception.CommentForbiddenException;
import com.halcyon.mediaservice.exception.CommentNotFoundException;
import com.halcyon.mediaservice.model.Comment;
import com.halcyon.mediaservice.model.Post;
import com.halcyon.mediaservice.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.halcyon.clients.util.UserUtil.isUserBanned;
import static com.halcyon.clients.util.UserUtil.isUserVerified;

@Service
@RequiredArgsConstructor
public class CommentService {
    @Value("${private.secret}")
    private String privateSecret;

    private final CommentRepository commentRepository;
    private final AuthProvider authProvider;
    private final UserClient userClient;
    private final PostService postService;

    public Comment create(CreateCommentDto dto) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);

        isUserBanned(user, "You are banned.");
        isUserVerified(user, "You are not verified. Please confirm your email.");

        Post post = postService.findById(dto.getPostId());
        Comment comment = Comment.builder()
                .content(dto.getContent())
                .authorId(user.getId())
                .post(post)
                .build();

        return commentRepository.save(comment);
    }

    public Comment create(CreateCommentByParentDto dto) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);

        isUserBanned(user, "You are banned.");
        isUserVerified(user, "You are not verified. Please confirm your email.");

        Comment parent = findById(dto.getParentId());

        Comment comment = Comment.builder()
                .content(dto.getContent())
                .authorId(user.getId())
                .post(parent.getPost())
                .parent(parent)
                .build();

        return commentRepository.save(comment);
    }

    public String delete(long commentId) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, "You are banned.");

        Comment comment = findById(commentId);

        if (comment.getAuthorId() != user.getId()) {
            throw new CommentForbiddenException();
        }

        commentRepository.delete(comment);
        return "The comment was successfully deleted";
    }

    public Comment findById(long commentId) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, "You are banned.");

        return commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);
    }

    public List<Comment> findAllByPost(long postId) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, "You are banned.");

        Post post = postService.findById(postId);
        return commentRepository.findAllByPost(post);
    }
}
