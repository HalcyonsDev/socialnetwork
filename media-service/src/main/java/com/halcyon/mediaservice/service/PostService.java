package com.halcyon.mediaservice.service;

import com.halcyon.clients.subscribe.SubscribeClient;
import com.halcyon.clients.subscribe.SubscriptionResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.clients.user.UserResponse;
import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.mediaservice.dto.CreatePostDto;
import com.halcyon.mediaservice.dto.UpdatePostDto;
import com.halcyon.mediaservice.exception.PostNotFoundException;
import com.halcyon.mediaservice.model.Post;
import com.halcyon.mediaservice.payload.NewPostMessage;
import com.halcyon.mediaservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.halcyon.mediaservice.util.UserUtil.isUserBanned;
import static com.halcyon.mediaservice.util.UserUtil.isUserVerified;

@Service
@RequiredArgsConstructor
public class PostService {
    @Value("${private.secret}")
    private String privateSecret;

    private final PostRepository postRepository;
    private final AuthProvider authProvider;
    private final UserClient userClient;
    private final SubscribeClient subscribeClient;
    private final MailActionsProducer mailActionsProducer;

    public Post create(CreatePostDto dto) {
        UserResponse userResponse = userClient.getByEmail(authProvider.getSubject(), privateSecret);

        isUserBanned(userResponse, "You are banned.");
        isUserVerified(userResponse, "You are not verified. Please confirm your email.");

        Post post = postRepository.save(new Post(dto.getTitle(), dto.getContent(), userResponse.getEmail()));

        List<SubscriptionResponse> subscribers = subscribeClient.getSubscriptions(userResponse.getEmail());
        NewPostMessage newPostMessage = new NewPostMessage(post.getId(), subscribers);
        mailActionsProducer.executeSendNewPostMessage(newPostMessage);

        return post;
    }

    public Post findById(long postId) {
        return postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    public Post update(UpdatePostDto dto) {
        Post post = findById(dto.getPostId());

        if (!post.getOwnerEmail().equals(authProvider.getSubject())) {
            throw new PostNotFoundException();
        }

        if (dto.getTitle() != null) {
            post.setTitle(dto.getTitle());
        }

        if (dto.getContent() != null) {
            post.setContent(dto.getContent());
        }

        return postRepository.save(post);
    }
}
