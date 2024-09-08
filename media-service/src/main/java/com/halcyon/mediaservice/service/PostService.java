package com.halcyon.mediaservice.service;

import com.halcyon.clients.subscription.SubscriptionClient;
import com.halcyon.clients.subscription.SubscriptionResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

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
    private final SubscriptionClient subscriptionClient;
    private final MailActionsProducer mailActionsProducer;

    private static final String BANNED_USER_MESSAGE = "You are banned.";

    public Post save(Post post) {
        return postRepository.save(post);
    }

    public Post create(CreatePostDto dto) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isValidUser(user);

        Post post = save(new Post(dto.getTitle(), dto.getContent(), user.getId()));
        sendNewPostMessage(user, post);

        return post;
    }

    private void sendNewPostMessage(PrivateUserResponse user, Post post) {
        List<SubscriptionResponse> subscribers = subscriptionClient.getSubscribers(user.getId());
        NewPostMessage newPostMessage = new NewPostMessage(post.getId(), subscribers);
        mailActionsProducer.executeSendNewPostMessage(newPostMessage);
    }

    private void isValidUser(PrivateUserResponse user) {
        isUserBanned(user, BANNED_USER_MESSAGE);
        isUserVerified(user, "You are not verified. Please confirm your email.");
    }

    public List<Post> getFeedForUser(int offset, int limit) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isValidUser(user);

        List<Integer> subscriptionsIds = subscriptionClient.getEmailsOfUsersSubscribedByUser(user.getId(), privateSecret);
        List<Post> feed = postRepository.findAllByOwnerIdIn(subscriptionsIds,
                PageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

        if (feed.size() < limit) {
            Page<Post> otherPosts = postRepository.findAllByOwnerIdNotIn(subscriptionsIds,
                    PageRequest.of(offset, limit - feed.size(), Sort.by(Sort.Direction.DESC, "createdAt")));
            feed = Stream.concat(feed.stream(), otherPosts.getContent().stream()).toList();
        }

        return feed;
    }

    public String delete(long postId) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, BANNED_USER_MESSAGE);

        Post post = findById(postId);

        if (post.getOwnerId() != user.getId()) {
            throw new PostForbiddenException();
        }

        postRepository.delete(post);
        commentRepository.deleteAllByPost(post);
        ratingRepository.deleteAllByPost(post);

        return "The post was successfully deleted.";
    }

    public Post getById(long postId) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, BANNED_USER_MESSAGE);

        return findById(postId);
    }

    public Post findById(long postId) {
        return postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    public Post update(UpdatePostDto dto) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, BANNED_USER_MESSAGE);

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

    public List<Post> getByPosts() {
        return getUserPosts(authProvider.getSubject());
    }

    public List<Post> getUserPosts(String email) {
        PrivateUserResponse user = userClient.getByEmail(email, privateSecret);
        isUserBanned(user, BANNED_USER_MESSAGE);

        return postRepository.findAllByOwnerId(user.getId());
    }
}
