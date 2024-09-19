package com.halcyon.userservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.halcyon.jwtlibrary.JwtProvider;
import com.halcyon.userservice.dto.CreateStrikeDto;
import com.halcyon.userservice.model.Strike;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.repository.StrikeRepository;
import com.halcyon.userservice.repository.UserRepository;
import com.halcyon.userservice.service.UserActionsConsumer;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StrikeControllerTests {
    @MockBean
    private UserActionsConsumer userActionsConsumer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StrikeRepository strikeRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MockMvc mockMvc;

    private static final String CAUSE_IS_REQUIRED_MESSAGE = "Cause is required.";
    private static final String BANNED_OWNER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_OWNER_MESSAGE = "You are not verified. Please confirm your email.";
    private static final String BANNED_TARGET_MESSAGE = "This user is already banned.";
    private static final String ALREADY_STRUCK_MESSAGE = "You have already struck to this user.";

    private static final String AUTH_HEADER = "Authorization";

    private static User owner;
    private static User target;

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

        owner = User.builder()
                .username("TestOwnerUsername")
                .email("test_owner@gmail.com")
                .about("TestOwnerAbout")
                .password("TestOwnerPassword123")
                .authProvider("local")
                .isVerified(true)
                .build();

        target = User.builder()
                .username("TestTargetUsername")
                .email("test_target@gmail.com")
                .about("TestTargetAbout")
                .password("TestTargetPassword123")
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
        strikeRepository.deleteAll();
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
    void create() throws Exception {
        User savedOwner = userRepository.save(owner);
        User savedTarget = userRepository.save(target);

        sendCreateRequest(getCreateStrikeDto())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("owner.id").value(savedOwner.getId()))
                .andExpect(jsonPath("target.id").value(savedTarget.getId()));
    }

    private ResultActions sendCreateRequest(CreateStrikeDto createStrikeDto) throws Exception {
        return mockMvc.perform(post("/api/v1/strikes")
                .header(AUTH_HEADER, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(createStrikeDto)));
    }

    private String getBearerToken() {
        return "Bearer " + jwtProvider.generateAccessToken(owner.getEmail());
    }

    private CreateStrikeDto getCreateStrikeDto() {
        return new CreateStrikeDto(target.getEmail(), "spam");
    }

    private String getRequestJson(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();

        return writer.writeValueAsString(object);
    }

    @Test
    void create_invalidDto() throws Exception {
        sendCreateRequest(getInvalidCreateStrikeDto())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("title").value("Bad Request"))
                .andExpect(jsonPath("status").value(400))
                .andExpect(jsonPath("detail").value(CAUSE_IS_REQUIRED_MESSAGE));
    }

    private CreateStrikeDto getInvalidCreateStrikeDto() {
        return new CreateStrikeDto(target.getEmail(), null);
    }

    @Test
    void create_alreadyExists() throws Exception {
        createStrike(owner, target);

        isValidExceptionResponse(
                sendCreateRequest(getCreateStrikeDto()),
                ALREADY_STRUCK_MESSAGE
        );
    }

    private Strike createStrike(User owner, User target) {
        User savedOwner = userRepository.save(owner);
        User savedTarget = userRepository.save(target);

        return strikeRepository.save(new Strike("spam", savedOwner, savedTarget));
    }

    @Test
    void create_bannedOwner() throws Exception {
        owner.setBanned(true);
        userRepository.save(owner);

        isValidExceptionResponse(
                sendCreateRequest(getCreateStrikeDto()),
                BANNED_OWNER_MESSAGE
        );

        owner.setBanned(false);
    }

    private void isValidExceptionResponse(ResultActions resultActions, String message) throws Exception{
        resultActions
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(message));
    }

    @Test
    void create_unverifiedUser() throws Exception {
        owner.setVerified(false);
        userRepository.save(owner);

        isValidExceptionResponse(
                sendCreateRequest(getCreateStrikeDto()),
                UNVERIFIED_OWNER_MESSAGE
        );

        owner.setVerified(true);
    }

    @Test
    void create_bannedTarget() throws Exception {
        target.setBanned(true);
        userRepository.save(owner);
        userRepository.save(target);

        isValidExceptionResponse(
                sendCreateRequest(getCreateStrikeDto()),
                BANNED_TARGET_MESSAGE
        );

        target.setBanned(false);
    }

    @Test
    void getSentStrikes() throws Exception {
        Strike strike = createStrike(owner, target);

        sendGetRequest("/api/v1/strikes/sent")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(strike.getId()));
    }

    private ResultActions sendGetRequest(String url) throws Exception {
        return mockMvc.perform(get(url)
                .header(AUTH_HEADER, getBearerToken()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/strikes/sent", "/api/v1/strikes/me"})
    void bannedOwner(String url) throws Exception {
        owner.setBanned(true);
        userRepository.save(owner);

        isValidExceptionResponse(
                sendGetRequest(url),
                BANNED_OWNER_MESSAGE
        );

        owner.setBanned(false);
    }

    @Test
    void getSentStrikes_unverifiedOwner() throws Exception {
        owner.setVerified(false);
        userRepository.save(owner);

        isValidExceptionResponse(
                sendGetRequest("/api/v1/strikes/sent"),
                UNVERIFIED_OWNER_MESSAGE
        );

        owner.setVerified(true);
    }

    @Test
    void getSentMeStrikes() throws Exception {
        Strike strike = createStrike(target, owner);

        sendGetRequest("/api/v1/strikes/me")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(strike.getId()));
    }
}
