package com.halcyon.mediaservice.service;

import com.halcyon.clients.exception.BannedUserException;
import com.halcyon.clients.exception.UnverifiedUserException;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.mediaservice.dto.CreateRatingDto;
import com.halcyon.mediaservice.dto.PostRatingsResponse;
import com.halcyon.mediaservice.dto.UpdateRatingDto;
import com.halcyon.mediaservice.exception.RatingAlreadyExistsException;
import com.halcyon.mediaservice.exception.RatingForbiddenException;
import com.halcyon.mediaservice.exception.RatingNotFoundException;
import com.halcyon.mediaservice.model.Post;
import com.halcyon.mediaservice.model.Rating;
import com.halcyon.mediaservice.repository.RatingRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceTests {
    @Mock
    private AuthProvider authProvider;

    @Mock
    private UserClient userClient;

    @Mock
    private PostService postService;

    @Mock
    private RatingRepository ratingRepository;

    @InjectMocks
    private RatingService ratingService;

    private static final String RATING_ALREADY_EXISTS_MESSAGE = "This user's rating for this post already exists.";
    private static final String BANNED_USER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_USER_MESSAGE = "You are not verified. Please confirm your email.";
    private static final String RATING_NOT_FOUND_MESSAGE = "Rating with this id not found.";
    private static final String SUCCESSFULLY_DELETED_RATING_MESSAGE = "The rating was successfully deleted.";
    private static final String FORBIDDEN_RATING_MESSAGE = "You don't have the rights to change this rating.";

    private static PrivateUserResponse user;
    private static Post post;

    @BeforeAll
    static void beforeAll() {
        user = PrivateUserResponse.builder()
                .id(1)
                .username("test_username")
                .email("test_user@gmail.com")
                .isVerified(true)
                .build();

        post = new Post("TestTitle", "TestContent", user.getId());
        post.setId(1L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void create(boolean isLike) {
        CreateRatingDto createRatingDto = getCreateRatingDto(isLike);
        mockCreating(createRatingDto);

        Rating rating = ratingService.create(createRatingDto);
        verify(postService).save(post);

        assertThat(rating).isNotNull();
        assertThat(rating.getPost()).isEqualTo(post);
        assertThat(rating.isLike()).isEqualTo(isLike);

        isValidRatingPost(isLike, rating.getPost());

        resetPostRatings();
    }

    private CreateRatingDto getCreateRatingDto(boolean isLike) {
        return new CreateRatingDto(post.getId(), isLike);
    }

    private void mockCreating(CreateRatingDto createRatingDto) {
        mockGettingUser();
        mockGettingPost();

        when(ratingRepository.existsByOwnerIdAndPost(user.getId(), post)).thenReturn(false);

        Rating rating = getRating(createRatingDto.getIsLike());
        when(ratingRepository.save(rating)).thenReturn(rating);
    }

    private void mockGettingUser() {
        when(authProvider.getSubject()).thenReturn(user.getEmail());
        when(userClient.getByEmail(user.getEmail(), null)).thenReturn(user);
    }

    private void mockGettingPost() {
        when(postService.findById(post.getId())).thenReturn(post);
    }

    private Rating getRating(boolean isLike) {
        return new Rating(isLike, user.getId(), post);
    }

    private void isValidRatingPost(boolean isLike, Post post) {
        if (isLike) {
            assertThat(post.getLikesCount()).isOne();
            assertThat(post.getDislikesCount()).isZero();
        } else {
            assertThat(post.getDislikesCount()).isOne();
            assertThat(post.getLikesCount()).isZero();
        }
    }

    private void resetPostRatings() {
        post.setLikesCount(0);
        post.setDislikesCount(0);
    }

    @Test
    void create_alreadyExists() {
        mockCreatingAlreadyExistsPost();
        CreateRatingDto createRatingDto = getCreateRatingDto(true);

        RatingAlreadyExistsException ratingAlreadyExistsException = assertThrows(RatingAlreadyExistsException.class,
                () -> ratingService.create(createRatingDto));
        assertThat(ratingAlreadyExistsException.getMessage()).isEqualTo(RATING_ALREADY_EXISTS_MESSAGE);
    }

    private void mockCreatingAlreadyExistsPost() {
        mockGettingUser();
        mockGettingPost();
        when(ratingRepository.existsByOwnerIdAndPost(user.getId(), post)).thenReturn(true);
    }

    @Test
    void create_bannedUser() {
        mockGettingBannedUser();

        CreateRatingDto createRatingDto = getCreateRatingDto(true);
        isValidBannedUserException(() -> ratingService.create(createRatingDto));

        user.setBanned(false);
    }

    private void isValidBannedUserException(Executable executable) {
        BannedUserException bannedUserException = assertThrows(BannedUserException.class, executable);
        assertThat(bannedUserException.getMessage()).isEqualTo(BANNED_USER_MESSAGE);
    }

    private void mockGettingBannedUser() {
        user.setBanned(true);
        mockGettingUser();
    }

    @Test
    void create_unverifiedUser() {
        user.setVerified(false);
        mockGettingUser();
        CreateRatingDto createRatingDto = getCreateRatingDto(true);

        UnverifiedUserException unverifiedUserException = assertThrows(UnverifiedUserException.class,
                () -> ratingService.create(createRatingDto));
        assertThat(unverifiedUserException.getMessage()).isEqualTo(UNVERIFIED_USER_MESSAGE);

        user.setVerified(true);
    }

    @Test
    void getById() {
        mockGettingPostById();

        Rating rating = ratingService.getById(1L);
        assertThat(rating).isNotNull();
        assertThat(rating.getPost()).isEqualTo(post);
        assertThat(rating.getOwnerId()).isEqualTo(user.getId());
    }

    private void mockGettingPostById() {
        mockGettingUser();
        mockFindingById(true);
    }

    private void mockFindingById(boolean isLike) {
        when(ratingRepository.findById(1L)).thenReturn(Optional.of(getRating(isLike)));
    }

    @Test
    void getById_notFound() {
        mockGettingNotFoundRating();

        RatingNotFoundException ratingNotFoundException = assertThrows(RatingNotFoundException.class,
                () -> ratingService.getById(1L));
        assertThat(ratingNotFoundException.getMessage()).isEqualTo(RATING_NOT_FOUND_MESSAGE);
    }

    private void mockGettingNotFoundRating() {
        mockGettingUser();
        when(ratingRepository.findById(1L)).thenReturn(Optional.empty());
    }

    @Test
    void getById_bannedUser() {
        mockGettingBannedUser();

        isValidBannedUserException(() -> ratingService.getById(1L));

        user.setBanned(false);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void delete(boolean isLike) {
        mockDeleting(isLike);

        String response = ratingService.delete(1L);

        verify(ratingRepository).delete(getRating(isLike));
        verify(postService).save(post);

        assertThat(response)
                .isNotNull()
                .isEqualTo(SUCCESSFULLY_DELETED_RATING_MESSAGE);

        assertThat(post.getLikesCount()).isZero();
        assertThat(post.getDislikesCount()).isZero();
    }

    private void mockDeleting(boolean isLike) {
        setRatingCount(isLike);
        mockGettingUser();
        mockFindingById(isLike);
    }

    private void setRatingCount(boolean isLike) {
        if (isLike) {
            post.setLikesCount(1);
        } else {
            post.setDislikesCount(1);
        }
    }

    @Test
    void delete_forbidden() {
        mockActionWithForbiddenRating();

        RatingForbiddenException ratingForbiddenException = assertThrows(RatingForbiddenException.class,
                () -> ratingService.delete(1L));
        assertThat(ratingForbiddenException.getMessage()).isEqualTo(FORBIDDEN_RATING_MESSAGE);
    }

    private void mockActionWithForbiddenRating() {
        mockGettingUser();

        Rating rating = getRating(true);
        rating.setOwnerId(2L);
        when(ratingRepository.findById(1L)).thenReturn(Optional.of(rating));
    }

    @Test
    void delete_bannedUser() {
        mockGettingBannedUser();

        isValidBannedUserException(() -> ratingService.delete(1L));

        user.setBanned(false);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void changeType(boolean lastType) {
        mockChangingType(lastType, !lastType);

        Rating rating = ratingService.changeType(getUpdateRatingDto(!lastType));
        verify(postService).save(post);

        assertThat(rating).isNotNull();

        if (lastType) {
            assertThat(post.getLikesCount()).isZero();
            assertThat(post.getDislikesCount()).isOne();
        } else {
            assertThat(post.getLikesCount()).isOne();
            assertThat(post.getDislikesCount()).isZero();
        }

        resetPostRatings();
    }

    private void mockChangingType(boolean lastType, boolean newType) {
        setRatingCount(lastType);
        mockGettingUser();
        mockFindingById(lastType);

        Rating rating = getRating(newType);
        when(ratingRepository.save(rating)).thenReturn(rating);
    }

    private UpdateRatingDto getUpdateRatingDto(boolean isLike) {
        return new UpdateRatingDto(1L, isLike);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void changeToSameType(boolean isLike) {
        mockChangingType(isLike, isLike);

        Rating rating = ratingService.changeType(getUpdateRatingDto(isLike));

        assertThat(rating).isNotNull();
        isValidRatingPost(isLike, rating.getPost());

        resetPostRatings();
    }

    @Test
    void changeType_forbidden() {
        mockActionWithForbiddenRating();

        UpdateRatingDto updateRatingDto = getUpdateRatingDto(true);
        RatingForbiddenException ratingForbiddenException = assertThrows(RatingForbiddenException.class,
                () -> ratingService.changeType(updateRatingDto));
        assertThat(ratingForbiddenException.getMessage()).isEqualTo(FORBIDDEN_RATING_MESSAGE);
    }

    @Test
    void getRatingsCountInPost() {
        when(postService.getById(post.getId())).thenReturn(post);
        PostRatingsResponse postRatingsResponse = ratingService.getRatingsCountInPost(post.getId());

        assertThat(postRatingsResponse).isNotNull();
        assertThat(postRatingsResponse.getPostId()).isEqualTo(post.getId());
        assertThat(postRatingsResponse.getLikesCount()).isEqualTo(post.getLikesCount());
        assertThat(postRatingsResponse.getDislikesCount()).isEqualTo(post.getDislikesCount());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void findRatingsInPost(boolean isLike) {
        when(postService.getById(post.getId())).thenReturn(post);

        ratingService.findRatingsInPost(post.getId(), isLike, 0, 5);

        verify(ratingRepository)
                .findAllByPostAndIsLike(post, isLike, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}
