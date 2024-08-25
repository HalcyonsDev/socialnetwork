package com.halcyon.authservice.security.oauth2;

import com.halcyon.authservice.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * Component repository class that extends {@link AuthorizationRequestRepository<OAuth2AuthorizationRequest>}
 * responsible for storing and retrieving OAuth2 authorization requests in HTTP cookies.
 *
 * @author Ruslan Sadikov
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    /**
     * Loads the OAuth2 authorization request from a cookie by deserializing each one {@link CookieUtil}
     *
     * @param request the {@link HttpServletRequest} from which to load the authorization request
     * @return the loaded {@link OAuth2AuthorizationRequest}, or {@code null} if not found
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtil.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> CookieUtil.deserialize(cookie, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    /**
     *  Saves the OAuth2 authorization request in a cookie.
     *  It stores the given {@link OAuth2AuthorizationRequest} as a serialized object in a cookie {@link CookieUtil}.
     *  If the authorization request is {@code null}, it removes any existing
     *  authorization request cookies. {@link #removeAuthorizationRequestCookies(HttpServletRequest, HttpServletResponse)}
     *  Additionally, if a redirectUri is present in the request, it is also stored in a separate cookie  {@link CookieUtil}.
     *
     * @param authorizationRequest the {@link OAuth2AuthorizationRequest} to be saved
     * @param request              the {@link HttpServletRequest} from which to retrieve the redirect URI parameter (if present)
     * @param response             the {@link HttpServletResponse} to which the cookies will be added
     */
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }

        CookieUtil.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, CookieUtil.serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);

        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (StringUtils.isNotBlank(redirectUriAfterLogin)) {
            CookieUtil.addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
        }
    }

    /**
     * Removes the OAuth2 authorization request by loading and returning it from cookies.
     *
     * @param request  the {@link HttpServletRequest} from which to load the authorization request
     * @param response the {@link HttpServletResponse} where the cookies are managed
     * @return the loaded {@link OAuth2AuthorizationRequest}, or {@code null} if no request was found in the cookies
     */
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return this.loadAuthorizationRequest(request);
    }

    /**
     * Removes the OAuth2 request cookies {@link CookieUtil}
     *
     * @param request  the {@link HttpServletRequest} from which to delete the cookies
     * @param response the {@link HttpServletResponse} where the cookies will be deleted
     */
    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtil.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }
}
