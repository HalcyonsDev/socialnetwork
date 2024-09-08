package com.halcyon.mediaservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.halcyon.clients.subscription.SubscriptionClient;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.JwtProvider;
import com.halcyon.mediaservice.dto.CreatePostDto;
import com.halcyon.mediaservice.dto.UpdatePostDto;
import com.halcyon.mediaservice.model.Post;
import com.halcyon.mediaservice.repository.PostRepository;
import com.halcyon.mediaservice.service.MailActionsProducer;
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

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostControllerTests {
    @Value("${private.secret}")
    private String privateSecret;

    @MockBean
    private UserClient userClient;

    @MockBean
    private MailActionsProducer mailActionsProducer;

    @MockBean
    private SubscriptionClient subscriptionClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MockMvc mockMvc;

    private static final String INVALID_TITLE_MESSAGE = "Title is required.";
    private static final String BANNED_USER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_USER_MESSAGE = "You are not verified. Please confirm your email.";
    private static final String DELETED_POST_MESSAGE = "The post was successfully deleted.";
    private static final String POST_FORBIDDEN_MESSAGE = "You don't have the rights to change this post.";

    private static final String AUTH_HEADER = "Authorization";

    private static PrivateUserResponse user;

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
        postRepository.deleteAll();
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().commands().flushAll();
    }

    @Test
    void connectionEstablished() {
        assertThat(postgres.isCreated()).isTrue();
        assertThat(postgres.isRunning()).isTrue();
        assertThat(redis.isCreated()).isTrue();
        assertThat(redis.isRunning()).isTrue();
    }

    @Test
    void create() throws Exception {
        mockGettingUser();
        CreatePostDto createPostDto = getCreatePostDto();

        sendCreatePostRequest(getCreatePostDto())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("title").value(createPostDto.getTitle()))
                .andExpect(jsonPath("content").value(createPostDto.getContent()))
                .andExpect(jsonPath("ownerId").value(user.getId()));
    }

    private void mockGettingUser() {
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);
    }

    private CreatePostDto getCreatePostDto() {
        return new CreatePostDto("TestTitle", "TestContent");
    }

    private ResultActions sendCreatePostRequest(CreatePostDto createPostDto) throws Exception {
        return mockMvc.perform(post("/api/v1/posts")
                .header(AUTH_HEADER, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(createPostDto)));
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

    @Test
    void create_invalidDto() throws Exception {
        mockGettingUser();

        sendCreatePostRequest(getInvalidCreatePostDto())
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("title").value("Bad Request"))
                .andExpect(jsonPath("status").value(400))
                .andExpect(jsonPath("detail").value(INVALID_TITLE_MESSAGE));
    }

    private void isValidExceptionResponse(ResultActions response, String message) throws Exception {
        response
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(message));
    }

    private CreatePostDto getInvalidCreatePostDto() {
        return new CreatePostDto(null, "TestContent");
    }

    @Test
    void create_bannedUser() throws Exception {
        mockGettingBannedUser();
        isValidBannedUserResponse(sendCreatePostRequest(getCreatePostDto()));
        user.setBanned(false);
    }

    private void isValidBannedUserResponse(ResultActions response) throws Exception {
        isValidExceptionResponse(response, BANNED_USER_MESSAGE);
    }

    private void mockGettingBannedUser() {
        user.setBanned(true);
        mockGettingUser();
    }

    @Test
    void create_unverifiedUser() throws Exception {
        mockGettingUnverifiedUser();
        isValidUnverifiedUser(sendCreatePostRequest(getCreatePostDto()));
        user.setVerified(true);
    }

    private void isValidUnverifiedUser(ResultActions response) throws Exception {
        isValidExceptionResponse(response, UNVERIFIED_USER_MESSAGE);
    }

    private void mockGettingUnverifiedUser() {
        user.setVerified(false);
        mockGettingUser();
    }
    
    @Test
    void getFeed() throws Exception {
        mockGettingFeed();
        savePosts();

        sendGetRequest("/api/v1/posts/feed")
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[9]").isNotEmpty())
                .andExpect(jsonPath("$[0].ownerId").value(4))
                .andExpect(jsonPath("$[3].ownerId").value(11));
    }

    private ResultActions sendGetRequest(String url) throws Exception {
        return mockMvc.perform(get(url)
                .header(AUTH_HEADER, getBearerToken()));
    }

    private void mockGettingFeed() {
        mockGettingUser();
        when(subscriptionClient.getEmailsOfUsersSubscribedByUser(user.getId(), privateSecret))
                .thenReturn(List.of(2, 3, 4));
    }

    private void savePosts() {
        for (int i = 2; i <= 11; i++) {
            postRepository.save(getPost(i));
        }
    }

    private Post getPost(long ownerId) {
        return new Post("TestTitle", "TestContent", ownerId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/posts/feed", "/api/v1/posts/1", "/api/v1/posts/my"})
    void bannedUser(String url) throws Exception {
        mockGettingBannedUser();
        isValidBannedUserResponse(sendGetRequest(url));
        user.setBanned(false);
    }

    @Test
    void getFeed_unverifiedUser() throws Exception {
        mockGettingUnverifiedUser();
        isValidUnverifiedUser(sendGetRequest("/api/v1/posts/feed"));
        user.setVerified(true);
    }

    @Test
    void deletePost() throws Exception {
        mockGettingUser();
        long postId = postRepository.save(getPost(user.getId())).getId();

        sendDeleteRequest(postId)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(DELETED_POST_MESSAGE));
    }

    private ResultActions sendDeleteRequest(long postId) throws Exception {
        return mockMvc.perform(delete("/api/v1/posts/" + postId)
                .header(AUTH_HEADER, getBearerToken()));
    }

    @Test
    void delete_forbiddenPost() throws Exception {
        mockGettingUser();
        long postId = postRepository.save(getPost(2L)).getId();

        isValidExceptionResponse(
                sendDeleteRequest(postId),
                POST_FORBIDDEN_MESSAGE
        );
    }

    @Test
    void delete_bannedUser() throws Exception {
        mockGettingBannedUser();
        isValidBannedUserResponse(sendDeleteRequest(1L));
        user.setBanned(false);
    }

    @Test
    void getById() throws Exception {
        mockGettingUser();
        long postId = postRepository.save(getPost(user.getId())).getId();

        sendGetRequest("/api/v1/posts/" + postId)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").value(postId));
    }

    @Test
    void update() throws Exception {
        mockGettingUser();

        long postId = postRepository.save(getPost(user.getId())).getId();
        UpdatePostDto updatePostDto = getUpdatePostDto(postId);

        sendUpdateRequest(updatePostDto)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").value(postId))
                .andExpect(jsonPath("title").value(updatePostDto.getTitle()))
                .andExpect(jsonPath("content").value(updatePostDto.getContent()));
    }

    private UpdatePostDto getUpdatePostDto(long postId) {
        return new UpdatePostDto(postId, "NewTitle", "NewContent");
    }

    private ResultActions sendUpdateRequest(UpdatePostDto updatePostDto) throws Exception {
        return mockMvc.perform(patch("/api/v1/posts/update")
                .header(AUTH_HEADER, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(updatePostDto)));
    }

    @Test
    void updateWithNull() throws Exception {
        mockGettingUser();

        Post post = postRepository.save(getPost(user.getId()));
        UpdatePostDto updatePostDto = new UpdatePostDto(post.getId(), null, "NewContent");

        sendUpdateRequest(updatePostDto)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").value(post.getId()))
                .andExpect(jsonPath("title").value(post.getTitle()))
                .andExpect(jsonPath("content").value(updatePostDto.getContent()));
    }

    @Test
    void update_bannedUser() throws Exception {
        mockGettingBannedUser();
        isValidBannedUserResponse(sendUpdateRequest(getUpdatePostDto(1L)));
        user.setBanned(false);
    }

    @Test
    void getMyPosts() throws Exception {
        mockGettingUser();
        saveUserPosts();

        sendGetRequest("/api/v1/posts/my")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[0].ownerId").value(user.getId()));
    }

    private void saveUserPosts() {
        for (int i = 0; i < 3; i++) {
            postRepository.save(getPost(user.getId()));
        }
    }

    @Test
    void getUserPosts() throws Exception {
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);
        saveUserPosts();

        sendGetUserPosts()
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[2].ownerId").value(user.getId()));
    }

    private ResultActions sendGetUserPosts() throws Exception {
        return mockMvc.perform(get("/api/v1/posts")
                .header(AUTH_HEADER, getBearerToken())
                .param("email", user.getEmail()));
    }

    @Test
    void getUserPosts_bannedUser() throws Exception {
        mockGettingBannedUser();
        isValidBannedUserResponse(sendGetUserPosts());
        user.setBanned(false);
    }
}
