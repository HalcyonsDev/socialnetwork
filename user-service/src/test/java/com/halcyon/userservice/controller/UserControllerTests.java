package com.halcyon.userservice.controller;

import com.halcyon.jwtlibrary.JwtProvider;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.repository.UserRepository;
import com.halcyon.userservice.service.FileStorageService;
import com.halcyon.userservice.service.UserActionsConsumer;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTests {
    @MockBean
    private UserActionsConsumer userActionsConsumer;

    @MockBean
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MockMvc mockMvc;

    private static final String USER_EMAIL_NOT_FOUND_MESSAGE = "User with this email not found.";
    private static final String USER_ID_NOT_FOUND_MESSAGE = "User with this id not found.";
    private static final String BANNED_USER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_USER_MESSAGE = "You are not verified. Please confirm your email.";

    private static final String AUTH_HEADER = "Authorization";
    private static final String NEW_USERNAME = "NewUsername";
    private static final String NEW_ABOUT = "NewAbout";

    private static User user;

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

        user = User.builder()
                .username("TestUsername")
                .email("test_user@gmail.com")
                .about("TestAbout")
                .password("TestPassword123")
                .authProvider("local")
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
        userRepository.deleteAll();
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
    void existsByEmail() throws Exception {
        userRepository.save(user);

        mockMvc.perform(get("/api/v1/users/exists")
                .param("email", user.getEmail()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("true"));
    }

    @Test
    void getById() throws Exception {
        User savedUser = userRepository.save(user);

        mockMvc.perform(get("/api/v1/users/" + savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").value(savedUser.getId()));
    }

    @Test
    void getById_userNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(USER_ID_NOT_FOUND_MESSAGE));
    }

    @Test
    void getByToken() throws Exception {
        User savedUser = userRepository.save(user);

        mockMvc.perform(get("/api/v1/users")
                .param("token", getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").value(savedUser.getId()));
    }

    private String getAccessToken() {
        return jwtProvider.generateAccessToken(user.getEmail());
    }

    @Test
    void getByToken_userNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .param("token", getAccessToken()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(USER_EMAIL_NOT_FOUND_MESSAGE));
    }

    @Test
    void uploadAvatar() throws Exception {
        User savedUser = userRepository.save(user);
        String avatar = "avatar_path";

        MockMultipartFile imageFile = getMockMultipartFile();
        when(fileStorageService.upload(imageFile)).thenReturn(avatar);

        mockMvc.perform(multipart("/api/v1/users/upload-avatar")
                .file(imageFile)
                .header(AUTH_HEADER, getBearerToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").value(savedUser.getId()))
                .andExpect(jsonPath("avatarPath").value(avatar));
    }

    private MockMultipartFile getMockMultipartFile() {
        return new MockMultipartFile(
                "avatar",
                "test-avatar.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "Test Image Content".getBytes()
        );
    }

    private String getBearerToken() {
        return "Bearer " + getAccessToken();
    }

    @Test
    void uploadAvatar_bannedUser() throws Exception {
        user.setBanned(true);
        userRepository.save(user);
        user.setBanned(false);

        MockMultipartFile imageFile = getMockMultipartFile();

       isUserBannedResponse(
               mockMvc.perform(multipart("/api/v1/users/upload-avatar")
                       .file(imageFile)
                       .header(AUTH_HEADER, getBearerToken()))
       );
    }

    private void isUserBannedResponse(ResultActions resultActions) throws Exception {
        resultActions
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(BANNED_USER_MESSAGE));
    }

    @Test
    void uploadAvatar_unverifiedUser() throws Exception {
        user.setVerified(false);
        userRepository.save(user);
        user.setVerified(true);

        MockMultipartFile imageFile = getMockMultipartFile();

        isUserUnverifiedResponse(
                mockMvc.perform(multipart("/api/v1/users/upload-avatar")
                        .file(imageFile)
                        .header(AUTH_HEADER, getBearerToken()))
        );
    }

    private void isUserUnverifiedResponse(ResultActions resultActions) throws Exception {
        resultActions
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(UNVERIFIED_USER_MESSAGE));
    }

    @Test
    void getMyAvatar() throws Exception {
        mockGetMyAvatar();

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/users/avatar/my")
                .header(AUTH_HEADER, getBearerToken()))
                .andExpect(status().isOk())
                .andReturn();

        isValidFileResponse(mvcResult);
    }

    private void mockGetMyAvatar() throws IOException {
        user.setAvatarPath("avatar_path");
        userRepository.save(user);

        File tempFile = File.createTempFile("avatar", ".png");
        when(fileStorageService.getFileByPath(user.getAvatarPath())).thenReturn(tempFile);
    }

    private void isValidFileResponse(MvcResult mvcResult) {
        byte[] fileContent = mvcResult.getResponse().getContentAsByteArray();
        assertThat(fileContent).isNotEmpty();
    }

    @Test
    void getAvatar() throws Exception {
        mockGetMyAvatar();

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/users/avatar")
                .param("email", user.getEmail()))
                .andExpect(status().isOk())
                .andReturn();

        isValidFileResponse(mvcResult);
    }

    @Test
    void updateUsername() throws Exception {
        User savedUser = userRepository.save(user);

        mockMvc.perform(patch("/api/v1/users/update-username")
                .header(AUTH_HEADER, getBearerToken())
                .param("username", NEW_USERNAME))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").value(savedUser.getId()))
                .andExpect(jsonPath("username").value(NEW_USERNAME));
    }

    @Test
    void updateUsername_bannedUser() throws Exception {
        user.setBanned(true);
        userRepository.save(user);
        user.setBanned(false);

        isUserBannedResponse(
                mockMvc.perform(patch("/api/v1/users/update-username")
                        .header(AUTH_HEADER, getBearerToken())
                        .param("username", NEW_USERNAME))
        );
    }

    @Test
    void updateUsername_unverifiedUser() throws Exception {
        user.setVerified(false);
        userRepository.save(user);
        user.setVerified(true);

        isUserUnverifiedResponse(
                mockMvc.perform(patch("/api/v1/users/update-username")
                        .header(AUTH_HEADER, getBearerToken())
                        .param("username", NEW_USERNAME))
        );
    }

    @Test
    void updateAbout() throws Exception {
        User savedUser = userRepository.save(user);

        performUpdateAboutRequest()
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").value(savedUser.getId()))
                .andExpect(jsonPath("about").value(NEW_ABOUT));
    }

    private ResultActions performUpdateAboutRequest() throws Exception {
        return mockMvc.perform(patch("/api/v1/users/update-about")
                .header(AUTH_HEADER, getBearerToken())
                .param("about", NEW_ABOUT));
    }

    @Test
    void updateAbout_bannedUser() throws Exception {
        user.setBanned(true);
        userRepository.save(user);
        user.setBanned(false);

        isUserBannedResponse(performUpdateAboutRequest());
    }

    @Test
    void updateAbout_unverifiedUser() throws Exception {
        user.setVerified(false);
        userRepository.save(user);
        user.setVerified(true);

        isUserUnverifiedResponse(performUpdateAboutRequest());
    }
}
