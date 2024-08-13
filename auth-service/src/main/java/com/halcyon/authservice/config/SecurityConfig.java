package com.halcyon.authservice.config;

import com.halcyon.authservice.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.halcyon.authservice.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.halcyon.authservice.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.halcyon.authservice.service.OAuth2UserService;
import com.halcyon.jwtlibrary.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2UserService oAuth2UserService;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Disabling csrf
                .csrf(AbstractHttpConfigurer::disable)

                // No session will be created or used
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(
                        auth -> auth
                                // Entry points
                                .requestMatchers(
                                        "/api/v1/auth/**",
                                        "/api/v1/2fa/login",
                                        "/oauth2/**"
                                ).permitAll()
                                .anyRequest().authenticated()
                )

                // OAuth2 settings
                .oauth2Login(customizer -> {
                    customizer.authorizationEndpoint(endpointConfig -> {
                        endpointConfig.baseUri("/oauth2/authorize");
                        endpointConfig.authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository);
                    });

                    customizer.redirectionEndpoint(endpointConfig ->
                            endpointConfig.baseUri("/oauth2/callback/*"));

                    customizer.userInfoEndpoint(endpointConfig ->
                            endpointConfig.userService(oAuth2UserService));

                    customizer.successHandler(oAuth2AuthenticationSuccessHandler);
                    customizer.failureHandler(oAuth2AuthenticationFailureHandler);
                })

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
