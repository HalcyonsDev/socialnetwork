package com.halcyon.mediaservice.service;

import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.mediaservice.dto.CreateChildCommentDto;
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

    private static final String BANNED_USER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_USER_MESSAGE = "You are not verified. Please confirm your email.";

    public Comment create(CreateCommentDto dto) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);

        isUserBanned(user, BANNED_USER_MESSAGE);
        isUserVerified(user, UNVERIFIED_USER_MESSAGE);

        Post post = postService.findById(dto.getPostId());
        Comment comment = Comment.builder()
                .content(dto.getContent())
                .authorId(user.getId())
                .post(post)
                .build();

        return commentRepository.save(comment);
    }

    public Comment create(CreateChildCommentDto dto) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);

        isUserBanned(user, BANNED_USER_MESSAGE);
        isUserVerified(user, UNVERIFIED_USER_MESSAGE);

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
        isUserBanned(user, BANNED_USER_MESSAGE);

        Comment comment = findById(commentId);

        if (comment.getAuthorId() != user.getId()) {
            throw new CommentForbiddenException();
        }

        commentRepository.delete(comment);
        return "The comment was successfully deleted";
    }

    public Comment getById(long commentId) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, BANNED_USER_MESSAGE);

        return findById(commentId);
    }

    public Comment findById(long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);
    }

    public List<Comment> findAllByPost(long postId) {
        Post post = postService.getById(postId);
        return commentRepository.findAllByPost(post);
    }
}
