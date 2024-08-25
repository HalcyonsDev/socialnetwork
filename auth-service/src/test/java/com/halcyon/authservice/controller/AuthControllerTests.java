package com.halcyon.authservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.halcyon.authservice.dto.RegisterUserDto;
import com.halcyon.authservice.dto.ResetPasswordDto;
import com.halcyon.authservice.payload.*;
import com.halcyon.authservice.security.RefreshTokenGenerator;
import com.halcyon.authservice.service.MailActionsProducer;
import com.halcyon.authservice.service.UserActionsProducer;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.JwtProvider;
import com.halcyon.rediscache.CacheManager;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {
    @Value("${private.secret}")
    private String privateSecret;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserClient userClient;

    @MockBean
    private UserActionsProducer userActionsProducer;

    @MockBean
    private MailActionsProducer mailActionsProducer;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RefreshTokenGenerator refreshTokenGenerator;

    static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7.4.0-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials provided.";
    private static final String INVALID_USERNAME_MESSAGE = "Username must start with an alphabet and contain only letters, digits, and spaces.";
    private static final String COMPLETE_2FA_MESSAGE = "Please, complete two-factor authentication.";
    private static final String SUCCESSFUL_LOGOUT_MESSAGE = "You have successfully logout from your account.";
    private static final String ACCOUNT_IS_VERIFIED_MESSAGE = "Account is verified.";
    private static final String VERIFICATION_CODE_MESSAGE = "The verification code will be sent to the email you specified.";
    private static final String INVALID_VERIFICATION_CODE_MESSAGE = "Invalid verification code.";
    private static final String USER_ALREADY_EXISTS_MESSAGE = "User with this email already exists.";
    private static final String FORGOT_PASSWORD_MESSAGE = "A link to reset your password has been sent to your email.";
    private static final String RESET_PASSWORD_MESSAGE = "Password has been reset successfully.";
    private static final String INVALID_PASSWORD_MESSAGE = "Password must contain at least one lowercase letter, one uppercase letter, and one digit.";
    private static final String BANNED_USER_MESSAGE = "You are banned.";

    private static final String AUTH_HEADER = "Authorization";
    public static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
    private static final String NEW_EMAIL = "new_email@gmail.com";
    private static final String NEW_PASSWORD = "NewPassword123";

    private static PrivateUserResponse user;

    @BeforeAll
    static void beforeAll() {
        redis.start();

        user = PrivateUserResponse.builder()
                .username("TestUsername")
                .email("test_user@gmail.com")
                .about("TestAbout")
                .password("TestPassword123")
                .isVerified(true)
                .build();
    }

    @AfterAll
    static void afterAll() {
        redis.stop();
    }

    @BeforeEach
    void setUp() {
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().commands().flushAll();
    }

    @Test
    void connectionEstablished() {
        assertThat(redis.isCreated()).isTrue();
        assertThat(redis.isRunning()).isTrue();
    }

    @Test
    void register() throws Exception {
        mockRegister();

        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getRequestJson(getRegisterUserDto())))
                        .andExpect(status().isOk())
                        .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        isValidAuthResponse(resultActions);
    }

    private void isValidAuthResponse(ResultActions resultActions) throws Exception {
        resultActions
                .andExpect(jsonPath("accessToken").isNotEmpty())
                .andExpect(jsonPath("refreshToken").isNotEmpty())
                .andExpect(jsonPath("type").value("Bearer"));
    }

    private void mockRegister() {
        mockGettingUser(getRegisterUserDto().getEmail());
        doNothing().when(userActionsProducer).executeCreatingUser(any(RegisterUserDto.class));
        doNothing().when(mailActionsProducer).executeSendVerificationMessage(any(VerificationMessage.class));
    }

    private void mockGettingUser(String email) {
        when(userClient.getByEmail(email, privateSecret)).thenReturn(user);
    }

    private RegisterUserDto getRegisterUserDto() {
        return RegisterUserDto.builder()
                .email("test@gmail.com")
                .username("TestUsername")
                .about("TestAboutMe")
                .password("TestPassword123")
                .build();
    }

    private String getRequestJson(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();

        return writer.writeValueAsString(object);
    }

    @Test
    void register_invalidDto() throws Exception {
        RegisterUserDto registerUserDto = getRegisterUserDto();
        registerUserDto.setUsername("?@!&");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(registerUserDto)))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("title").value("Bad Request"))
                .andExpect(jsonPath("status").value(400))
                .andExpect(jsonPath("detail").value(INVALID_USERNAME_MESSAGE));
    }

    @Test
    void login() throws Exception {
        mockLogin();

        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getRequestJson(getAuthRequest())))
                        .andExpect(status().isOk())
                        .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        isValidAuthResponse(resultActions);
    }

    private void mockLogin() {
        AuthRequest authRequest = getAuthRequest();

        mockGettingUser(authRequest.getEmail());
        when(passwordEncoder.matches(eq(authRequest.getPassword()), any(String.class))).thenReturn(true);
    }

    private AuthRequest getAuthRequest() {
        return new AuthRequest(user.getEmail(), user.getPassword());
    }

    @Test
    void login_isUsing2FA() throws Exception {
        mockLogin_isUsing2FA();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(getAuthRequest())))
                .andExpect(status().isOk())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(COMPLETE_2FA_MESSAGE));

        user.setUsing2FA(false);
    }

    private void mockLogin_isUsing2FA() {
        user.setUsing2FA(true);
        mockLogin();
    }

    @Test
    void login_invalidCredentials() throws Exception {
        mockLogin_invalidCredentials();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(getAuthRequest())))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(INVALID_CREDENTIALS_MESSAGE));
    }

    private void mockLogin_invalidCredentials() {
        AuthRequest authRequest = getAuthRequest();

        mockGettingUser(authRequest.getEmail());
        when(passwordEncoder.matches(eq(authRequest.getPassword()), any(String.class))).thenReturn(false);
    }

    @Test
    void logout() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/logout")
                .header(AUTH_HEADER, getBearerToken()))
                .andExpect(status().isOk())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(SUCCESSFUL_LOGOUT_MESSAGE));
    }

    private String getBearerToken() {
        return "Bearer " + getJwtAccessToken();
    }

    private String getJwtAccessToken() {
        return jwtProvider.generateAccessToken(user.getEmail());
    }
    
    @Test
    void getAccessToken() throws Exception {
        mockMvc.perform(put("/api/v1/auth/access")
                .header(REFRESH_TOKEN_HEADER, getJwtRefreshToken()))
                .andExpect(status().isOk())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("accessToken").isNotEmpty())
                .andExpect(jsonPath("refreshToken").isEmpty())
                .andExpect(jsonPath("type").value("Bearer"));
    }

    private String getJwtRefreshToken() {
        return refreshTokenGenerator.generate(user.getEmail());
    }

    @Test
    void getRefreshToken() throws Exception {
        ResultActions resultActions = mockMvc.perform(put("/api/v1/auth/refresh")
                        .header(REFRESH_TOKEN_HEADER, getJwtRefreshToken()))
                        .andExpect(status().isOk())
                        .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        isValidAuthResponse(resultActions);
    }

    @Test
    void confirmEmail() throws Exception {
        mockConfirmEmail();

        mockMvc.perform(get("/api/v1/auth")
                .param("token", getJwtAccessToken()))
                .andExpect(status().isOk())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(ACCOUNT_IS_VERIFIED_MESSAGE));
    }

    private void mockConfirmEmail() {
        doNothing().when(userActionsProducer).executeConfirmByEmail(user.getEmail());
    }

    @Test
    void changeEmail() throws Exception {
        mockChangeEmail();

        mockMvc.perform(patch("/api/v1/auth/change-email")
                .param("email", NEW_EMAIL))
                .andExpect(status().isOk())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(VERIFICATION_CODE_MESSAGE));
    }

    private void mockChangeEmail() {
        when(userClient.existsByEmail(user.getEmail())).thenReturn(false);
        doNothing().when(mailActionsProducer).executeSendVerificationMessage(any(VerificationMessage.class));
    }

    @Test
    void changeEmail_userAlreadyExists() throws Exception {
        saveEmailAndCodeInCache(1111);
        mockChangeEmail();

        mockMvc.perform(patch("/api/v1/auth/change-email")
                .param("email", NEW_EMAIL))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(USER_ALREADY_EXISTS_MESSAGE));
    }

    private void saveEmailAndCodeInCache(int verificationCode) {
        cacheManager.save(NEW_EMAIL, verificationCode, Duration.ofHours(1));
    }

    @Test
    void confirmEmailChange() throws Exception {
        saveEmailAndCodeInCache(1111);

        mockConfirmEmailChange();

        ResultActions resultActions = mockMvc.perform(patch("/api/v1/auth/confirm-change-email")
                        .header(AUTH_HEADER, getBearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getRequestJson(getConfirmEmailChangeRequest())))
                        .andExpect(status().isOk())
                        .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        isValidAuthResponse(resultActions);
    }

    private ConfirmEmailChangeRequest getConfirmEmailChangeRequest() {
        return new ConfirmEmailChangeRequest(1111, NEW_EMAIL);
    }

    private void mockConfirmEmailChange() {
        doNothing().when(userActionsProducer).executeChangeEmail(any(ChangeEmailMessage.class));
    }

    @Test
    void confirmEmailChange_invalidVerificationCode() throws Exception {
        saveEmailAndCodeInCache(2222);

        mockConfirmEmail();

        mockMvc.perform(patch("/api/v1/auth/confirm-change-email")
                .header(AUTH_HEADER, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(getConfirmEmailChangeRequest())))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(INVALID_VERIFICATION_CODE_MESSAGE));
    }

    @Test
    void forgotPassword() throws Exception {
        mockForgotPassword();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .header(AUTH_HEADER, getBearerToken()))
                .andExpect(status().isOk())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(FORGOT_PASSWORD_MESSAGE));
    }

    private void mockForgotPassword() {
        mockGettingUser(user.getEmail());
        doNothing().when(mailActionsProducer).executeSendForgotPasswordMessage(any(ForgotPasswordMessage.class));
    }

    @Test
    void resetPassword() throws Exception {
        mockResetPassword();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .param("token", getJwtAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(getResetPasswordDto())))
                .andExpect(status().isOk())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(RESET_PASSWORD_MESSAGE));
    }

    private ResetPasswordDto getResetPasswordDto() {
        return new ResetPasswordDto(user.getPassword(), NEW_PASSWORD);
    }

    private void mockResetPassword() {
        mockGettingUser(user.getEmail());
        when(passwordEncoder.matches(user.getPassword(), user.getPassword())).thenReturn(true);
        doNothing().when(userActionsProducer).executeResetPassword(any(UserPasswordResetMessage.class));
    }

    @Test
    void resetPassword_invalidDto() throws Exception {
        ResetPasswordDto resetPasswordDto = getResetPasswordDto();
        resetPasswordDto.setNewPassword("invalid_password");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .param("token", getJwtAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(resetPasswordDto)))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("title").value("Bad Request"))
                .andExpect(jsonPath("status").value(400))
                .andExpect(jsonPath("detail").value(INVALID_PASSWORD_MESSAGE));
    }

    @Test
    void resetPassword_invalidCredentials() throws Exception {
        mockResetPassword_invalidCredentials();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .param("token", getJwtAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(getResetPasswordDto())))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(INVALID_CREDENTIALS_MESSAGE));
    }

    private void mockResetPassword_invalidCredentials() {
        mockGettingUser(user.getEmail());
        when(passwordEncoder.matches(user.getPassword(), user.getPassword())).thenReturn(false);
    }

    @Test
    void resetPassword_bannedUser() throws Exception {
        mockResetPassword_bannedUser();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .param("token", getJwtAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getRequestJson(getResetPasswordDto())))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("reason").value(BANNED_USER_MESSAGE));

        user.setBanned(false);
    }

    private void mockResetPassword_bannedUser() {
        user.setBanned(true);
        mockGettingUser(user.getEmail());
    }
}
