package com.halcyon.authservice.security.oauth2;

import com.halcyon.authservice.security.oauth2.user.UserPrincipal;
import com.halcyon.authservice.util.CookieUtil;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

import static com.halcyon.authservice.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

/**
 * Component class that extends {@link SimpleUrlAuthenticationSuccessHandler}
 * responsible for handling successful OAuth2 authentication
 *
 * @author Ruslan Sadikov
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Value("${private.secret}")
    private String privateSecret;

    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final UserClient userClient;
    private final JwtProvider jwtProvider;

    /**
     * Handles the actions to be taken after a successful OAuth2 authentication.
     * It determines the target URL {@link #determineTargetUrl(HttpServletRequest, HttpServletResponse, Authentication)},
     * clears authentication attribute {@link #clearAuthenticationAttributes(HttpServletRequest, HttpServletResponse)},
     * and redirects the user to the target URL with an access token as a query parameter.
     *
     * @param request        the {@link HttpServletRequest} from which to extract information
     * @param response       the {@link HttpServletResponse} to send the response
     * @param authentication the {@link Authentication} containing user details
     * @throws IOException if an input or output error occurs while handling the success
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) return;

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * Determines the target URL after successful authentication.
     * This method retrieves the redirect URI from a  {@link CookieUtil}
     * or uses the default target URL if none is found {@link #getDefaultTargetUrl()}.
     * It then generates a JWT access token for the authenticated user {@link JwtProvider}
     * and appends it as a query parameter to the target URL.
     *
     * @param request        the {@link HttpServletRequest} from which to extract information
     * @param response       the {@link HttpServletResponse} to send the response
     * @param authentication the {@link Authentication} containing user details
     * @return the URL to which the user should be redirected
     */
    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> redirectUri = CookieUtil.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        PrivateUserResponse user = userClient.getByEmail(userPrincipal.getEmail(), privateSecret);

        String accessToken = jwtProvider.generateAccessToken(user.getEmail());

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("token", accessToken)
                .build().toString();
    }

    /**
     * Clears the temporary authentication-related attributes {@link #clearAuthenticationAttributes(HttpServletRequest)}
     * and removes authorization request cookies {@link HttpCookieOAuth2AuthorizationRequestRepository}.
     *
     * @param request  the {@link HttpServletRequest} from which to extract information
     * @param response the {@link HttpServletResponse} to send the response
     */
    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
}
