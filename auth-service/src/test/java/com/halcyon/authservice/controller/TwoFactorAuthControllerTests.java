package com.halcyon.authservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.halcyon.authservice.dto.Login2FADto;
import com.halcyon.authservice.dto.Verify2FADto;
import com.halcyon.authservice.payload.SaveSecretMessage;
import com.halcyon.authservice.service.UserActionsProducer;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.JwtProvider;
import com.redis.testcontainers.RedisContainer;
import org.jboss.aerogear.security.otp.api.Base32;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TwoFactorAuthControllerTests {
    @Value("${private.secret}")
    private String privateSecret;

    @MockBean
    private UserClient userClient;

    @MockBean
    private UserActionsProducer userActionsProducer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.4");
    private static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7.4.0-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    private static final String BANNED_USER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_USER_MESSAGE = "You are not verified. Please confirm your email.";
    private static final String INVALID_VERIFICATION_CODE_MESSAGE = "Invalid verification code (otp)";
    private static final String TWO_FACTOR_IS_NOT_REQUIRED_MESSAGE = "Two-factor authentication is not required.";

    private static final String AUTH_HEADER = "Authorization";
    private static final String OTP = "00000";


    private static PrivateUserResponse user;

    @BeforeAll
    static void beforeAll() {
        postgres.start();
        redis.start();

        user = PrivateUserResponse.builder()
                .username("TestUsername")
                .email("test_user@gmail.com")
                .about("TestAbout")
                .password("TestPassword123")
                .isVerified(true)
                .secret(Base32.random())
                .build();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
        redis.stop();
    }

    @BeforeEach
    void setUp() {
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
    void setup2FA() throws Exception {
        mockSetup2FA();

        mockMvc.perform(post("/api/v1/2fa/setup")
                .header(AUTH_HEADER, getBearerToken()))
                .andExpect(status().isOk())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("qrCodeUrl").isNotEmpty());
    }

    private String getBearerToken() {
        return "Bearer " + getJwtAccessToken();
    }

    private String getJwtAccessToken() {
        return jwtProvider.generateAccessToken(user.getEmail());
    }

    private void mockSetup2FA() {
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);
        doNothing().when(userActionsProducer).executeSaveSecret(any(SaveSecretMessage.class));
    }

    @Test
    void setup2FA_bannedUser() throws Exception {
        mockSetup2FA_bannedUser();
        performSetup2FAErrorRequest(BANNED_USER_MESSAGE);

        user.setBanned(false);
    }

    private void mockSetup2FA_bannedUser() {
        user.setBanned(true);
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);
    }

    private void performSetup2FAErrorRequest(String message) throws Exception {
        mockMvc.perform(post("/api/v1/2fa/setup")
                .header(AUTH_HEADER, getBearerToken()))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(message));
    }

    @Test
    void setup2FA_unverifiedUser() throws Exception {
        mockSetup2FA_unverifiedUser();
        performSetup2FAErrorRequest(UNVERIFIED_USER_MESSAGE);

        user.setVerified(true);
    }

    private void mockSetup2FA_unverifiedUser() {
        user.setVerified(false);
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);
    }

    @Test
    void verify2FA_invalidCredentials() throws Exception {
        mockVerify2FA_invalidCredentials();

        mockMvc.perform(post("/api/v1/2fa/verify")
                .header(AUTH_HEADER, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(new Verify2FADto(OTP))))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(INVALID_VERIFICATION_CODE_MESSAGE));
    }

    private void mockVerify2FA_invalidCredentials() {
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);
    }

    private String getRequestJson(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();

        return writer.writeValueAsString(object);
    }

    @Test
    void login_2faIsNotRequired() throws Exception {
        mockLogin_2faIsNotRequired();

        mockMvc.perform(post("/api/v1/2fa/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(getLogin2FADto())))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(TWO_FACTOR_IS_NOT_REQUIRED_MESSAGE));
    }

    private void mockLogin_2faIsNotRequired() {
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);
    }

    private Login2FADto getLogin2FADto() {
        return new Login2FADto(user.getEmail(), OTP);
    }
}
