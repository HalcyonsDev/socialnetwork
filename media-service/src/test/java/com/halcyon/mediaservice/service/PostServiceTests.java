package com.halcyon.mediaservice.service;

import com.halcyon.clients.exception.BannedUserException;
import com.halcyon.clients.exception.UnverifiedUserException;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTests {
    @Mock
    private AuthProvider authProvider;

    @Mock
    private UserClient userClient;

    @Mock
    private SubscriptionClient subscriptionClient;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private MailActionsProducer mailActionsProducer;

    @InjectMocks
    private PostService postService;

    private static final String BANNED_USER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_USER_MESSAGE = "You are not verified. Please confirm your email.";
    private static final String POST_FORBIDDEN_MESSAGE = "You don't have the rights to change this post.";
    private static final String POST_SUCCESSFULLY_DELETED_MESSAGE = "The post was successfully deleted.";
    private static final String POST_NOT_FOUND_MESSAGE = "Post with this id not found.";

    private static PrivateUserResponse user;

    @BeforeAll
    static void beforeAll() {
        user = PrivateUserResponse.builder()
                .id(1)
                .username("test_username")
                .email("test_user@gmail.com")
                .isVerified(true)
                .build();
    }

    @Test
    void create() {
        mockCreating();

        Post post = postService.create(getCreatePostDto());

        assertThat(post)
                .isNotNull()
                .isEqualTo(getPost());
    }

    private void mockCreating() {
        Post post = getPost();

        mockGettingUser();
        when(postRepository.save(any(Post.class))).thenReturn(post);
        mockSendingNewPostMessage();
    }

    private Post getPost() {
        Post post = new Post("TestTitle", "TestContent", 1);
        post.setId(1L);

        return post;
    }

    private void mockGettingUser() {
        when(authProvider.getSubject()).thenReturn(user.getEmail());
        when(userClient.getByEmail(user.getEmail(), null)).thenReturn(user);
    }

    private void mockSendingNewPostMessage() {
        List<SubscriptionResponse> subscribers = List.of(new SubscriptionResponse());
        when(subscriptionClient.getSubscribers(user.getId())).thenReturn(subscribers);

        NewPostMessage newPostMessage = new NewPostMessage(1L, subscribers);
        doNothing().when(mailActionsProducer).executeSendNewPostMessage(newPostMessage);
    }

    private CreatePostDto getCreatePostDto() {
        return new CreatePostDto("TestTitle", "TestContent");
    }

    @Test
    void create_bannedUser() {
        isValidBannedUserException(() -> postService.create(getCreatePostDto()));
    }

    private void isValidBannedUserException(Executable executable) {
        user.setBanned(true);
        mockGettingUser();

        BannedUserException bannedUserException = assertThrows(BannedUserException.class, executable);
        assertThat(bannedUserException.getMessage()).isEqualTo(BANNED_USER_MESSAGE);

        user.setBanned(false);
    }

    @Test
    void create_unverifiedUser() {
        isValidUnverifiedUserException(() -> postService.create(getCreatePostDto()));
    }

    private void isValidUnverifiedUserException(Executable executable) {
        user.setVerified(false);
        mockGettingUser();

        UnverifiedUserException unverifiedUserException = assertThrows(UnverifiedUserException.class, executable);
        assertThat(unverifiedUserException.getMessage()).isEqualTo(UNVERIFIED_USER_MESSAGE);

        user.setVerified(true);
    }

    @Test
    void getFeedForUser() {
        mockGettingFeed();
        List<Post> posts = postService.getFeedForUser(0, 3);

        assertThat(posts).isNotNull();
    }

    private void mockGettingFeed() {
        mockGettingUser();

        List<Integer> subscriptions = List.of(1, 2, 3);
        when(subscriptionClient.getEmailsOfUsersSubscribedByUser(user.getId(), null))
                .thenReturn(subscriptions);

        Page<Post> posts = mock(Page.class);
        when(postRepository.findAllByOwnerIdIn(List.of(1, 2, 3),
                PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(posts);

        Page<Post> otherPosts = mock(Page.class);
        when(postRepository.findAllByOwnerIdNotIn(subscriptions,
                PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(otherPosts);
    }

    @Test
    void getFeedForUser_bannedUser() {
        isValidBannedUserException(() -> postService.getFeedForUser(0, 10));
    }

    @Test
    void getFeedForUser_unverifiedUser() {
        isValidUnverifiedUserException(() -> postService.getFeedForUser(0, 10));
    }

    @Test
    void delete() {
        mockDeleting(getPost());
        String response = postService.delete(1);

        Post post = getPost();
        verify(postRepository).delete(post);
        verify(commentRepository).deleteAllByPost(post);
        verify(ratingRepository).deleteAllByPost(post);

        assertThat(response)
                .isNotNull()
                .isEqualTo(POST_SUCCESSFULLY_DELETED_MESSAGE);
    }

    private void mockDeleting(Post post) {
        mockGettingUser();
        mockFinding(post);
    }

    private void mockFinding(Post post) {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
    }

    @Test
    void delete_bannedUser() {
        isValidBannedUserException(() -> postService.delete(1));
    }

    @Test
    void delete_postForbidden() {
        Post post = getPost();
        post.setOwnerId(2);
        mockDeleting(post);

        PostForbiddenException postForbiddenException = assertThrows(PostForbiddenException.class,
                () -> postService.delete(1));
        assertThat(postForbiddenException.getMessage()).isEqualTo(POST_FORBIDDEN_MESSAGE);
    }

    @Test
    void getById() {
        long postId = 1L;

        mockGettingUser();
        mockFinding(getPost());

        Post post = postService.getById(postId);
        assertThat(post)
                .isNotNull()
                .isEqualTo(getPost());
    }

    @Test
    void getById_notFound() {
        mockGettingUser();
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        PostNotFoundException postNotFoundException = assertThrows(PostNotFoundException.class,
                () -> postService.getById(1L));
        assertThat(postNotFoundException.getMessage()).isEqualTo(POST_NOT_FOUND_MESSAGE);
    }

    @Test
    void getById_bannedUser() {
        isValidBannedUserException(() -> postService.getById(1L));
    }

    @Test
    void update() {
        mockUpdating(getPost());

        UpdatePostDto updatePostDto = getUpdatePostDto();
        Post updatedPost = postService.update(getUpdatePostDto());

        assertThat(updatedPost).isNotNull();
        assertThat(updatedPost.getTitle()).isEqualTo(updatePostDto.getTitle());
        assertThat(updatedPost.getContent()).isEqualTo(updatePostDto.getContent());
    }

    private void mockUpdating(Post post) {
        mockGettingUser();
        mockFinding(post);
        when(postRepository.save(post)).thenReturn(post);
    }

    private UpdatePostDto getUpdatePostDto() {
        return new UpdatePostDto(1L, "NewPostTitle", "NewPostContent");
    }

    @Test
    void update_postForbidden() {
        Post post = getPost();
        post.setOwnerId(2L);

        mockGettingUser();
        mockFinding(post);

        UpdatePostDto updatePostDto = getUpdatePostDto();
        PostForbiddenException postForbiddenException = assertThrows(PostForbiddenException.class,
                () -> postService.update(updatePostDto));
        assertThat(postForbiddenException.getMessage()).isEqualTo(POST_FORBIDDEN_MESSAGE);
    }

    @Test
    void update_bannedUser() {
        isValidBannedUserException(() -> postService.update(getUpdatePostDto()));
    }

    @Test
    void findMyPosts() {
        mockGettingUser();

        postService.getByPosts();
        verify(postRepository).findAllByOwnerId(user.getId());
    }

    @Test
    void findMyPosts_bannedUser() {
        isValidBannedUserException(() -> postService.getByPosts());
    }

    @Test
    void findUserPosts() {
        when(userClient.getByEmail(user.getEmail(), null)).thenReturn(user);

        postService.getUserPosts(user.getEmail());
        verify(postRepository).findAllByOwnerId(user.getId());
    }

    @Test
    void findUserPosts_bannedUser() {
        user.setBanned(true);
        when(userClient.getByEmail(user.getEmail(), null)).thenReturn(user);

        String email = user.getEmail();
        BannedUserException bannedUserException = assertThrows(BannedUserException.class,
                () -> postService.getUserPosts(email));
        assertThat(bannedUserException.getMessage()).isEqualTo(BANNED_USER_MESSAGE);

        user.setBanned(false);
    }
}
