package com.halcyon.mediaservice.service;

import com.halcyon.clients.subscribe.SubscribeClient;
import com.halcyon.clients.subscribe.SubscriptionResponse;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.mediaservice.dto.CreatePostDto;
import com.halcyon.mediaservice.dto.UpdatePostDto;
import com.halcyon.mediaservice.exception.PostForbiddenException;
import com.halcyon.mediaservice.exception.PostNotFoundException;
import com.halcyon.mediaservice.model.Post;
import com.halcyon.mediaservice.payload.NewPostMessage;
import com.halcyon.mediaservice.repository.CommentRepository;
import com.halcyon.mediaservice.repository.PostRepository;
import com.halcyon.mediaservice.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.halcyon.clients.util.UserUtil.isUserBanned;
import static com.halcyon.clients.util.UserUtil.isUserVerified;

@Service
@RequiredArgsConstructor
public class PostService {
    @Value("${private.secret}")
    private String privateSecret;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RatingRepository ratingRepository;
    private final AuthProvider authProvider;
    private final UserClient userClient;
    private final SubscribeClient subscribeClient;
    private final MailActionsProducer mailActionsProducer;

    public Post save(Post post) {
        return postRepository.save(post);
    }

    public Post create(CreatePostDto dto) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);

        isUserBanned(user, "You are banned.");
        isUserVerified(user, "You are not verified. Please confirm your email.");

        Post post = save(new Post(dto.getTitle(), dto.getContent(), user.getId()));

        List<SubscriptionResponse> subscribers = subscribeClient.getSubscriptions(user.getId());
        NewPostMessage newPostMessage = new NewPostMessage(post.getId(), subscribers);
        mailActionsProducer.executeSendNewPostMessage(newPostMessage);

        return post;
    }

    public String delete(long postId) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, "You are banned.");

        Post post = findById(postId);

        if (post.getOwnerId() != user.getId()) {
            throw new PostForbiddenException();
        }

        postRepository.delete(post);
        commentRepository.deleteAllByPost(post);
        ratingRepository.deleteAllByPost(post);

        return "The post was successfully deleted.";
    }

    public Post findById(long postId) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, "You are banned.");

        return postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    public Post update(UpdatePostDto dto) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, "You are banned.");

        Post post = findById(dto.getPostId());

        if (post.getOwnerId() != user.getId()) {
            throw new PostForbiddenException();
        }

        if (dto.getTitle() != null) {
            post.setTitle(dto.getTitle());
        }

        if (dto.getContent() != null) {
            post.setContent(dto.getContent());
        }

        return save(post);
    }

    public List<Post> findMyPosts() {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, "You are banned.");

        return findUserPosts(authProvider.getSubject());
    }

    public List<Post> findUserPosts(String email) {
        PrivateUserResponse user = userClient.getByEmail(email, privateSecret);
        isUserBanned(user, "You are banned.");
        isUserVerified(user, "You are not verified. Please confirm your email.");

        return postRepository.findAllByOwnerEmail(email);
    }
}
