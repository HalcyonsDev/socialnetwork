package com.halcyon.mediaservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.JwtProvider;
import com.halcyon.mediaservice.dto.CreateRatingDto;
import com.halcyon.mediaservice.dto.UpdateRatingDto;
import com.halcyon.mediaservice.model.Post;
import com.halcyon.mediaservice.model.Rating;
import com.halcyon.mediaservice.repository.PostRepository;
import com.halcyon.mediaservice.repository.RatingRepository;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RatingControllerTests {
    @Value("${private.secret}")
    private String privateSecret;

    @MockBean
    private UserClient userClient;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MockMvc mockMvc;

    private static final String RATING_ALREADY_EXISTS_MESSAGE = "This user's rating for this post already exists.";
    private static final String BANNED_USER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_USER_MESSAGE = "You are not verified. Please confirm your email.";
    private static final String SUCCESSFULLY_DELETED_RATING_MESSAGE = "The rating was successfully deleted.";
    private static final String FORBIDDEN_RATING_MESSAGE = "You don't have the rights to change this rating.";

    private static final String AUTH_HEADER = "Authorization";

    private static PrivateUserResponse user;
    private static Post post;

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.4");

    static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7.4.0-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeAll
    static void beforeAll() {
        postgres.start();
        redis.start();

        user = PrivateUserResponse.builder()
                .id(1L)
                .email("test_user@gmail.com")
                .username("TestUsername")
                .isVerified(true)
                .build();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
        redis.stop();
    }

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        postRepository.deleteAll();
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().commands().flushAll();
        post = new Post("TestTitle", "TestContent", user.getId());
    }

    @Test
    void connectionEstablished() {
        assertThat(postgres.isCreated()).isTrue();
        assertThat(postgres.isRunning()).isTrue();
        assertThat(redis.isCreated()).isTrue();
        assertThat(redis.isRunning()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void create(boolean isLike) throws Exception {
        mockGettingUser();
        long postId = postRepository.save(post).getId();

        CreateRatingDto createRatingDto = getCreateRatingDto(isLike, postId);

        ResultActions resultActions = sendCreateRequest(createRatingDto)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("ownerId").value(user.getId()))
                .andExpect(jsonPath("post.id").value(postId))
                .andExpect(jsonPath("like").value(isLike));

        isValidRatingsCounts(isLike, resultActions);
    }

    private void mockGettingUser() {
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);
    }

    private CreateRatingDto getCreateRatingDto(boolean isLike, long postId) {
        return new CreateRatingDto(postId, isLike);
    }

    private ResultActions sendCreateRequest(CreateRatingDto createRatingDto) throws Exception {
        return mockMvc.perform(post("/api/v1/ratings")
                .header(AUTH_HEADER, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(createRatingDto)));
    }

    private String getBearerToken() {
        return "Bearer " + jwtProvider.generateAccessToken(user.getEmail());
    }

    private String getRequestJson(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();

        return writer.writeValueAsString(object);
    }

    private void isValidRatingsCounts(boolean isLike, ResultActions resultActions) throws Exception {
        if (isLike) {
            resultActions
                    .andExpect(jsonPath("post.likesCount").value(1))
                    .andExpect(jsonPath("post.dislikesCount").value(0));
        } else {
            resultActions
                    .andExpect(jsonPath("post.likesCount").value(0))
                    .andExpect(jsonPath("post.dislikesCount").value(1));
        }
    }

    @Test
    void create_alreadyExists() throws Exception {
        mockGettingUser();
        post = postRepository.save(post);
        saveRating(true, post);

        isValidExceptionResponse(
                sendCreateRequest(getCreateRatingDto(true, post.getId())),
                RATING_ALREADY_EXISTS_MESSAGE
        );
    }

    private Rating saveRating(boolean isLike, Post post) {
        return ratingRepository.save(new Rating(isLike, user.getId(), post));
    }

    private void isValidExceptionResponse(ResultActions response, String message) throws Exception {
        response
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(message));
    }

    @Test
    void create_bannedUser() throws Exception {
        mockGettingBannedUser();
        isValidExceptionResponse(
                sendCreateRequest(getCreateRatingDto(true, 1L)),
                BANNED_USER_MESSAGE
        );
        user.setBanned(false);
    }

    private void mockGettingBannedUser() {
        user.setBanned(true);
        mockGettingUser();
    }

    @Test
    void create_unverifiedUser() throws Exception {
        user.setVerified(false);
        mockGettingUser();

        isValidExceptionResponse(
                sendCreateRequest(getCreateRatingDto(true, 1L)),
                UNVERIFIED_USER_MESSAGE
        );

        user.setVerified(true);
    }

    @Test
    void getById() throws Exception {
        mockGettingUser();
        post = postRepository.save(post);

        long ratingId = saveRating(true, post).getId();
        sendGetRequest("/api/v1/ratings/" + ratingId)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").value(ratingId));
    }

    private ResultActions sendGetRequest(String url) throws Exception {
        return mockMvc.perform(get(url)
                .header(AUTH_HEADER, getBearerToken()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/ratings/1", "/api/v1/ratings/likes/post/1", "/api/v1/ratings/dislikes/post/1"})
    void bannedUser(String url) throws Exception {
        mockGettingBannedUser();
        isValidExceptionResponse(sendGetRequest(url), BANNED_USER_MESSAGE);
        user.setBanned(false);
    }

    @Test
    void deleteRating() throws Exception {
        mockGettingUser();

        long ratingId = saveRating(true, postRepository.save(post)).getId();
        sendDeleteRequest(ratingId)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(SUCCESSFULLY_DELETED_RATING_MESSAGE));
    }

    private ResultActions sendDeleteRequest(long ratingId) throws Exception {
        return mockMvc.perform(delete("/api/v1/ratings/" + ratingId)
                .header(AUTH_HEADER, getBearerToken()));
    }

    @Test
    void delete_forbidden() throws Exception {
        long ratingId = saveRating(true, postRepository.save(post)).getId();

        user.setId(2L);
        mockGettingUser();

        isValidExceptionResponse(
                sendDeleteRequest(ratingId),
                FORBIDDEN_RATING_MESSAGE
        );

        user.setId(1L);
    }

    @Test
    void delete_bannedUser() throws Exception {
        mockGettingBannedUser();
        isValidExceptionResponse(sendDeleteRequest(1L), BANNED_USER_MESSAGE);
        user.setBanned(false);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void changeType(boolean isLike) throws Exception {
        mockGettingUser();

        if (isLike) {
            post.setLikesCount(1);
        } else {
            post.setDislikesCount(1);
        }

        post = postRepository.save(post);
        long ratingId = saveRating(isLike, post).getId();
        UpdateRatingDto updateRatingDto = new UpdateRatingDto(ratingId, !isLike);

        ResultActions resultActions = sendChangeTypeRequest(updateRatingDto)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").value(ratingId))
                .andExpect(jsonPath("post.id").value(post.getId()));

        isValidRatingsCounts(!isLike, resultActions);
    }

    private ResultActions sendChangeTypeRequest(UpdateRatingDto updateRatingDto) throws Exception {
        return mockMvc.perform(patch("/api/v1/ratings/change-type")
                .header(AUTH_HEADER, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(updateRatingDto)));
    }

    @Test
    void changeType_forbidden() throws Exception {
        long ratingId = saveRating(true, postRepository.save(post)).getId();

        user.setId(2L);
        mockGettingUser();

        isValidExceptionResponse(
                sendChangeTypeRequest(new UpdateRatingDto(ratingId, true)),
                FORBIDDEN_RATING_MESSAGE
        );

        user.setId(1L);
    }

    @Test
    void changeType_bannedUser() throws Exception {
        mockGettingBannedUser();

        isValidExceptionResponse(
                sendChangeTypeRequest(new UpdateRatingDto(1L, true)),
                BANNED_USER_MESSAGE
        );

        user.setBanned(false);
    }

    @Test
    void getRatingsCountInPost() throws Exception {
        mockGettingUser();

        post.setLikesCount(3);
        post.setDislikesCount(3);
        post = postRepository.save(post);

        saveRatings(true);
        saveRatings(false);

        sendGetRequest("/api/v1/ratings/post/" + post.getId())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("postId").value(post.getId()))
                .andExpect(jsonPath("likesCount").value(3))
                .andExpect(jsonPath("dislikesCount").value(3));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getRatingsInPost(boolean isLike) throws Exception {
        mockGettingUser();

        post = postRepository.save(post);
        saveRatings(isLike);

        String url = isLike ? "/api/v1/ratings/likes/post/" : "/api/v1/ratings/dislikes/post/";
        sendGetRequest(url + post.getId())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("content").isNotEmpty())
                .andExpect(jsonPath("content[2].like").value(isLike));
    }

    private void saveRatings(boolean isLike) {
        for (int i = 0; i < 3; i++) {
            saveRating(isLike, post);
        }
    }
}
