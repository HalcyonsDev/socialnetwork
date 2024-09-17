package com.halcyon.mediaservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.JwtProvider;
import com.halcyon.mediaservice.dto.CreateChildCommentDto;
import com.halcyon.mediaservice.dto.CreateCommentDto;
import com.halcyon.mediaservice.model.Comment;
import com.halcyon.mediaservice.model.Post;
import com.halcyon.mediaservice.repository.CommentRepository;
import com.halcyon.mediaservice.repository.PostRepository;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class CommentControllerTests {
    @Value("${private.secret}")
    private String privateSecret;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserClient userClient;

    private static final String AUTH_HEADER = "Authorization";

    private static final String BANNED_USER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_USER_MESSAGE = "You are not verified. Please confirm your email.";
    private static final String SUCCESSFULLY_DELETED_MESSAGE = "The comment was successfully deleted";
    private static final String FORBIDDEN_COMMENT_MESSAGE = "You don't have the rights to change this comment.";

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
        commentRepository.deleteAll();
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

    @Test
    void createParentComment() throws Exception {
        mockGettingUser();
        long postId = postRepository.save(post).getId();
        CreateCommentDto createCommentDto = getCreateCommentDto(postId);

        sendPostRequest("/api/v1/comments", createCommentDto)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("content").value(createCommentDto.getContent()))
                .andExpect(jsonPath("authorId").value(user.getId()))
                .andExpect(jsonPath("post.id").value(postId))
                .andExpect(jsonPath("parent").isEmpty());
    }

    private void mockGettingUser() {
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);
    }

    private CreateCommentDto getCreateCommentDto(long postId) {
        return new CreateCommentDto(postId, "TestContent");
    }

    private ResultActions sendPostRequest(String url, Object requestBody) throws Exception {
        return mockMvc.perform(post(url)
                .header(AUTH_HEADER, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(requestBody)));
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
    void createParentComment_bannedUser() throws Exception {
        mockGettingBannedUser();

        isValidExceptionResponse(
                sendPostRequest("/api/v1/comments", getCreateCommentDto(1L)),
                BANNED_USER_MESSAGE
        );

        user.setBanned(false);
    }

    private void mockGettingBannedUser() {
        user.setBanned(true);
        mockGettingUser();
    }

    private void isValidExceptionResponse(ResultActions response, String message) throws Exception {
        response
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(message));
    }

    @Test
    void createParentComment_unverifiedUser() throws Exception {
        mockGettingUnverifiedUser();

        isValidExceptionResponse(
                sendPostRequest("/api/v1/comments", getCreateCommentDto(1L)),
                UNVERIFIED_USER_MESSAGE
        );

        user.setVerified(true);
    }

    private void mockGettingUnverifiedUser() {
        user.setVerified(false);
        mockGettingUser();
    }

    @Test
    void createChildComment() throws Exception {
        mockGettingUser();
        long parentId = saveParent().getId();
        CreateChildCommentDto createChildCommentDto = getCreateChildCommentDto(parentId);

        sendPostRequest("/api/v1/comments/by-parent", createChildCommentDto)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("content").value(createChildCommentDto.getContent()))
                .andExpect(jsonPath("authorId").value(user.getId()))
                .andExpect(jsonPath("post.id").value(post.getId()))
                .andExpect(jsonPath("parent.id").value(parentId));

    }

    private Comment saveParent() {
        post = postRepository.save(post);
        return commentRepository.save(getParentComment());
    }

    private Comment getParentComment() {
        return Comment.builder()
                .content("TestParentContent")
                .authorId(user.getId())
                .post(post)
                .build();
    }

    private CreateChildCommentDto getCreateChildCommentDto(long parentId) {
        return new CreateChildCommentDto(parentId, "TestContent");
    }

    @Test
    void createChildComment_bannedUser() throws Exception {
        mockGettingBannedUser();

        isValidExceptionResponse(
                sendPostRequest("/api/v1/comments/by-parent", getCreateChildCommentDto(1L)),
                BANNED_USER_MESSAGE
        );

        user.setBanned(false);
    }

    @Test
    void createChildComment_unverifiedUser() throws Exception {
        mockGettingUnverifiedUser();

        isValidExceptionResponse(
                sendPostRequest("/api/v1/comments/by-parent", getCreateChildCommentDto(1L)),
                UNVERIFIED_USER_MESSAGE
        );

        user.setVerified(true);
    }

    @Test
    void deleteComment() throws Exception {
        mockGettingUser();
        long commentId = saveParent().getId();

        sendDeleteRequest(commentId)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(SUCCESSFULLY_DELETED_MESSAGE));

        assertThat(commentRepository.existsById(commentId)).isFalse();
    }

    private ResultActions sendDeleteRequest(long commentId) throws Exception {
        return mockMvc.perform(delete("/api/v1/comments/" + commentId)
                .header(AUTH_HEADER, getBearerToken()));
    }

    @Test
    void delete_forbidden() throws Exception {
        long commentId = saveParent().getId();

        user.setId(2L);
        mockGettingUser();

        isValidExceptionResponse(sendDeleteRequest(commentId), FORBIDDEN_COMMENT_MESSAGE);
        user.setId(1L);
    }

    @Test
    void delete_bannedUser() throws Exception {
        mockGettingBannedUser();
        isValidExceptionResponse(sendDeleteRequest(1L), BANNED_USER_MESSAGE);
        user.setBanned(false);
    }

    @Test
    void getById() throws Exception {
        mockGettingUser();
        long commentId = saveParent().getId();

        sendGetByIdRequest("/api/v1/comments/" + commentId)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").value(commentId));
    }

    private ResultActions sendGetByIdRequest(String url) throws Exception {
        return mockMvc.perform(get(url)
                .header(AUTH_HEADER, getBearerToken()));
    }

    @Test
    void getById_bannedUser() throws Exception {
        mockGettingBannedUser();
        isValidExceptionResponse(sendGetByIdRequest("/api/v1/comments/1"), BANNED_USER_MESSAGE);
        user.setBanned(false);
    }

    @Test
    void getCommentsInPost() throws Exception {
        mockGettingUser();
        saveComments();

        sendGetCommentsInPostRequest()
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[2].id").isNotEmpty());
    }

    private void saveComments() {
        post = postRepository.save(post);
        for (int i = 0; i < 3; i++) {
            commentRepository.save(getParentComment());
        }
    }

    private ResultActions sendGetCommentsInPostRequest() throws Exception {
        return mockMvc.perform(get("/api/v1/comments")
                .header(AUTH_HEADER, getBearerToken())
                .param("postId", String.valueOf(post.getId())));
    }

    @Test
    void getCommentsInPost_bannedUser() throws Exception {
        mockGettingBannedUser();

        post = postRepository.save(post);
        isValidExceptionResponse(sendGetCommentsInPostRequest(), BANNED_USER_MESSAGE);

        user.setBanned(false);
    }
}
