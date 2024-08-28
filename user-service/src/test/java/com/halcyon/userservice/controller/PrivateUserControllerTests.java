package com.halcyon.userservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.halcyon.userservice.dto.RegisterOAuth2UserDto;
import com.halcyon.userservice.dto.UpdateOAuth2UserDto;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.repository.UserRepository;
import com.halcyon.userservice.service.UserActionsConsumer;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrivateUserControllerTests {
    @Value("${private.secret}")
    private String privateSecret;

    @MockBean
    private UserActionsConsumer userActionsConsumer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MockMvc mockMvc;

    private static final String PRIVATE_SECRET_HEADER = "PrivateSecret";

    private static final String NOT_PRESENT_PRIVATE_SECRET_MESSAGE = "Required header 'PrivateSecret' is not present.";
    private static final String BAD_HEADER_CREDENTIALS_MESSAGE = "Bad Request Header Credentials.";
    private static final String USER_EMAIL_NOT_FOUND_MESSAGE = "User with this email not found.";
    private static final String USER_ID_NOT_FOUND_MESSAGE = "User with this id not found.";

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
                .avatarPath("TestAvatar")
                .authProvider("google")
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
    void getByEmail() throws Exception {
        User savedUser = userRepository.save(user);

        isValidUserResponse(
                mockMvc.perform(get("/api/v1/users/private")
                        .header(PRIVATE_SECRET_HEADER, privateSecret)
                        .param("email", user.getEmail())),
                savedUser.getId()
        );
    }

    private void isValidUserResponse(ResultActions resultActions, long userId) throws Exception {
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").value(userId));
    }

    @Test
    void getByEmail_notPresentPrivateSecret() throws Exception {
        userRepository.save(user);

        isValidNotPresentPrivateSecretResponse(
                mockMvc.perform(get("/api/v1/users/private")
                        .param("email", user.getEmail()))
        );
    }

    private void isValidNotPresentPrivateSecretResponse(ResultActions resultActions) throws Exception {
        resultActions
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("title").value("Bad Request"))
                .andExpect(jsonPath("detail").value(NOT_PRESENT_PRIVATE_SECRET_MESSAGE));
    }

    @Test
    void getByEmail_invalidPrivateSecret() throws Exception {
        userRepository.save(user);

        isValidInvalidPrivateSecretResponse(
                mockMvc.perform(get("/api/v1/users/private")
                        .header(PRIVATE_SECRET_HEADER, "invalid_secret")
                        .param("email", user.getEmail()))
        );
    }

    private void isValidInvalidPrivateSecretResponse(ResultActions resultActions) throws Exception {
        resultActions
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(BAD_HEADER_CREDENTIALS_MESSAGE));
    }

    @Test
    void getByEmail_userNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/private")
                .header(PRIVATE_SECRET_HEADER, privateSecret)
                .param("email", user.getEmail()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(USER_EMAIL_NOT_FOUND_MESSAGE));
    }

    @Test
    void getById() throws Exception {
        User savedUser = userRepository.save(user);

        isValidUserResponse(
                mockMvc.perform(get("/api/v1/users/private/" + savedUser.getId())
                        .header(PRIVATE_SECRET_HEADER, privateSecret)),
                savedUser.getId()
        );
    }

    @Test
    void getById_notPresentPrivateSecret() throws Exception {
        User savedUser = userRepository.save(user);

        isValidNotPresentPrivateSecretResponse(
                mockMvc.perform(get("/api/v1/users/private/" + savedUser.getId()))
        );
    }

    @Test
    void getById_invalidPrivateSecret() throws Exception {
        User savedUser = userRepository.save(user);

        isValidInvalidPrivateSecretResponse(
                mockMvc.perform(get("/api/v1/users/private/" + savedUser.getId())
                        .header(PRIVATE_SECRET_HEADER, "invalid_secret"))
        );
    }

    @Test
    void getById_userNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/private/1")
                .header(PRIVATE_SECRET_HEADER, privateSecret))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(USER_ID_NOT_FOUND_MESSAGE));
    }

    @Test
    void registerOAuth2User() throws Exception {
        RegisterOAuth2UserDto registerOAuth2UserDto = getRegisterOAuth2UserDto();

        mockMvc.perform(post("/api/v1/users/private")
                .header(PRIVATE_SECRET_HEADER, privateSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(registerOAuth2UserDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("email").value(registerOAuth2UserDto.getEmail()))
                .andExpect(jsonPath("username").value(registerOAuth2UserDto.getUsername()))
                .andExpect(jsonPath("avatarPath").value(registerOAuth2UserDto.getAvatarUrl()))
                .andExpect(jsonPath("authProvider").value(registerOAuth2UserDto.getAuthProvider()));
    }

    private RegisterOAuth2UserDto getRegisterOAuth2UserDto() {
        return new RegisterOAuth2UserDto(
                user.getEmail(),
                user.getUsername(),
                user.getAvatarPath(),
                user.getAuthProvider()
        );
    }

    private String getRequestJson(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();

        return writer.writeValueAsString(object);
    }

    @Test
    void registerOAuth2User_notPresentPrivateSecret() throws Exception {
        isValidNotPresentPrivateSecretResponse(
                mockMvc.perform(post("/api/v1/users/private")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getRequestJson(getRegisterOAuth2UserDto())))
        );
    }

    @Test
    void registerOAuth2User_invalidPrivateSecret() throws Exception {
        isValidInvalidPrivateSecretResponse(
                mockMvc.perform(post("/api/v1/users/private")
                        .header(PRIVATE_SECRET_HEADER, "invalid_secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getRequestJson(getRegisterOAuth2UserDto())))
        );
    }

    @Test
    void updateOAuth2UserData() throws Exception {
        User savedUser = userRepository.save(user);
        UpdateOAuth2UserDto updateOAuth2UserDto = getUpdateOAuth2UserDto();

        mockMvc.perform(post("/api/v1/users/private/update-data")
                .header(PRIVATE_SECRET_HEADER, privateSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(updateOAuth2UserDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").value(savedUser.getId()))
                .andExpect(jsonPath("email").value(updateOAuth2UserDto.getEmail()))
                .andExpect(jsonPath("username").value(updateOAuth2UserDto.getUsername()))
                .andExpect(jsonPath("avatarPath").value(updateOAuth2UserDto.getAvatarUrl()));
    }

    private UpdateOAuth2UserDto getUpdateOAuth2UserDto() {
        return new UpdateOAuth2UserDto(
                user.getEmail(),
                "NewUsername",
                "NewAvatarUrl"
        );
    }

    @Test
    void updateOAuth2UserData_notPresentPrivateSecret() throws Exception {
        isValidNotPresentPrivateSecretResponse(
                mockMvc.perform(post("/api/v1/users/private/update-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getRequestJson(getUpdateOAuth2UserDto()))
                )
        );
    }

    @Test
    void updateOAuth2UserData_invalidPrivateSecret() throws Exception {
        isValidInvalidPrivateSecretResponse(
                mockMvc.perform(post("/api/v1/users/private/update-data")
                        .header(PRIVATE_SECRET_HEADER, "invalid_secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getRequestJson(getUpdateOAuth2UserDto())))
        );
    }
}
