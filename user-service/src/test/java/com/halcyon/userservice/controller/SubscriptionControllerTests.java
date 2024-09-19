package com.halcyon.userservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.halcyon.jwtlibrary.JwtProvider;
import com.halcyon.userservice.dto.SubscriptionDto;
import com.halcyon.userservice.model.Subscription;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.repository.SubscriptionRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionControllerTests {
    @MockBean
    private UserActionsConsumer userActionsConsumer;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MockMvc mockMvc;

    private static final String USER_NOT_FOUND_MESSAGE = "User with this email not found.";
    private static final String SUBSCRIPTION_ALREADY_EXISTS_MESSAGE = "You have already subscribed to this user";
    private static final String BANNED_USER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_USER_MESSAGE = "Unverified users do not have the option to subscribe. Please confirm your email.";
    private static final String SUCCESSFULLY_UNSUBSCRIBED_MESSAGE = "You have successfully unsubscribed.";
    private static final String SUBSCRIPTION_NOT_FOUND_MESSAGE = "Subscription with this owner and target is not found.";
    private static final String SUBSCRIPTION_ID_NOT_FOUND_MESSAGE = "Subscription with this id not found.";

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
        subscriptionRepository.deleteAll();
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
    void subscribe() throws Exception {
        User savedOwner = userRepository.save(owner);
        User savedTarget = userRepository.save(target);

        sendSubscribeRequest()
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("owner.id").value(savedOwner.getId()))
                .andExpect(jsonPath("target.id").value(savedTarget.getId()));
    }

    private ResultActions sendSubscribeRequest() throws Exception {
        return mockMvc.perform(post("/api/v1/subscriptions/subscribe")
                .header(AUTH_HEADER, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(getSubscriptionDto())));
    }

    private String getBearerToken() {
        return "Bearer " + jwtProvider.generateAccessToken(owner.getEmail());
    }

    private String getRequestJson(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();

        return writer.writeValueAsString(object);
    }

    private SubscriptionDto getSubscriptionDto() {
        return new SubscriptionDto(target.getEmail());
    }

    @Test
    void subscribe_targetNotFound() throws Exception {
        userRepository.save(owner);

        isValidExceptionResponse(
                sendSubscribeRequest(),
                USER_NOT_FOUND_MESSAGE
        );
    }

    private void isValidExceptionResponse(ResultActions response, String message) throws Exception {
        response
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(message));
    }

    @Test
    void subscribe_alreadyExists() throws Exception {
        saveSubscription();

        isValidExceptionResponse(
                sendSubscribeRequest(),
                SUBSCRIPTION_ALREADY_EXISTS_MESSAGE
        );
    }

    private Subscription saveSubscription() {
        User savedOwner = userRepository.save(owner);
        User savedTarget = userRepository.save(target);

        return subscriptionRepository.save(new Subscription(savedOwner, savedTarget));
    }

    @Test
    void subscribe_bannedUser() throws Exception {
        owner.setBanned(true);
        userRepository.save(owner);

        isValidExceptionResponse(
                sendSubscribeRequest(),
                BANNED_USER_MESSAGE
        );

        owner.setBanned(false);
    }

    @Test
    void subscribe_unverifiedUser() throws Exception {
        owner.setVerified(false);
        userRepository.save(owner);

        isValidExceptionResponse(
                sendSubscribeRequest(),
                UNVERIFIED_USER_MESSAGE
        );

        owner.setVerified(true);
    }

    @Test
    void unsubscribe() throws Exception {
        saveSubscription();

        sendUnsubscribeRequest()
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(SUCCESSFULLY_UNSUBSCRIBED_MESSAGE));
    }

    private ResultActions sendUnsubscribeRequest() throws Exception {
        return mockMvc.perform(delete("/api/v1/subscriptions/unsubscribe")
                .header(AUTH_HEADER, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(getSubscriptionDto())));
    }

    @Test
    void unsubscribe_notFound() throws Exception {
        userRepository.save(owner);
        userRepository.save(target);

        isValidExceptionResponse(
                sendUnsubscribeRequest(),
                SUBSCRIPTION_NOT_FOUND_MESSAGE
        );
    }

    @Test
    void unsubscribe_bannedUser() throws Exception {
        owner.setBanned(true);
        userRepository.save(owner);

        isValidExceptionResponse(
                sendUnsubscribeRequest(),
                BANNED_USER_MESSAGE
        );

        owner.setBanned(false);
    }

    @Test
    void getById() throws Exception {
        long savedSubscriptionId = saveSubscription().getId();

        sendGetRequest("/api/v1/subscriptions/" + savedSubscriptionId)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").value(savedSubscriptionId));
    }

    private ResultActions sendGetRequest(String url) throws Exception {
        return mockMvc.perform(get(url)
                .header(AUTH_HEADER, getBearerToken()));
    }

    @Test
    void getById_notFound() throws Exception {
        userRepository.save(owner);
        userRepository.save(target);

        isValidExceptionResponse(
                sendGetRequest("/api/v1/subscriptions/1"),
                SUBSCRIPTION_ID_NOT_FOUND_MESSAGE
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/subscriptions/1", "/api/v1/subscriptions", "/api/v1/subscriptions/owner/1", "/api/v1/subscriptions/subscribers/target/1", "/api/v1/subscriptions/subscribers"})
    void bannedUser(String url) throws Exception {
        owner.setBanned(true);
        userRepository.save(owner);

        isValidExceptionResponse(sendGetRequest(url), BANNED_USER_MESSAGE);

        owner.setBanned(false);
    }

    @Test
    void getById_bannedUser() throws Exception {
        owner.setBanned(true);
        userRepository.save(owner);

        isValidExceptionResponse(
                sendGetRequest("/api/v1/subscriptions/1"),
                BANNED_USER_MESSAGE
        );

        owner.setBanned(false);
    }

    @Test
    void getSubscriptions() throws Exception {
        Subscription savedSubscription = saveSubscription();

        sendGetRequest("/api/v1/subscriptions")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[0].id").value(savedSubscription.getId()));
    }

    @Test
    void getSubscriptionsByOwnerId() throws Exception {
        User savedOwner = userRepository.save(owner);
        User savedTarget = userRepository.save(target);
        Subscription savedSubscription = subscriptionRepository.save(new Subscription(savedOwner, savedTarget));

        sendGetRequest("/api/v1/subscriptions/owner/" + savedOwner.getId())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[0].id").value(savedSubscription.getId()));
    }

    @Test
    void findSubscribers() throws Exception {
        User savedUser = userRepository.save(owner);
        User savedTarget = userRepository.save(target);
        Subscription savedSubscription = subscriptionRepository.save(new Subscription(savedTarget, savedUser));

        sendGetRequest("/api/v1/subscriptions/subscribers")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[0].id").value(savedSubscription.getId()));
    }

    @Test
    void getSubscribersByTargetId() throws Exception {
        User savedOwner = userRepository.save(owner);
        User savedTarget = userRepository.save(target);
        Subscription savedSubscription = subscriptionRepository.save(new Subscription(savedOwner, savedTarget));

        sendGetRequest("/api/v1/subscriptions/subscribers/target/" + savedTarget.getId())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[0].id").value(savedSubscription.getId()));
    }
}
