package com.halcyon.authservice.security.oauth2;

import com.halcyon.authservice.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

import static com.halcyon.authservice.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

/**
 * Component class that extends {@link SimpleUrlAuthenticationFailureHandler}
 * responsible for handling OAuth2 authentication failures by redirecting
 * the user to the specified target URL with an error message.
 *
 * @author Ruslan Sadikov
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    /**
     * Handles an OAuth2 authentication failure event {@link AuthenticationException}.
     * It retrieves {@link CookieUtil} and redirects the user to the target URL from a cookie,
     * appends the localized error message as a query parameter,
     * removes the OAuth2 authorization request cookies {@link HttpCookieOAuth2AuthorizationRequestRepository}.
     *
     * @param request   the {@link HttpServletRequest} that resulted in an {@link AuthenticationException}
     * @param response  the {@link HttpServletResponse} so the user agent can be advised of the failure
     * @param exception the {@link AuthenticationException} that caused the authentication to fail
     * @throws IOException if an input or output error occurs while handling the failure
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        String targetUrl = CookieUtil.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse("/");

        targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", exception.getLocalizedMessage())
                .build().toString();

        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
