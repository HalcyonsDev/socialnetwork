package com.halcyon.userservice.security;

import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class RequestHeaderAuthenticationProvider implements AuthenticationProvider {
    @Value("${private.secret}")
    private String privateSecret;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        System.out.println("!!!!");
        String authSecretKey = String.valueOf(authentication.getPrincipal());

        if (StringUtils.isBlank(authSecretKey) || !authSecretKey.equals(privateSecret)) {
            throw new BadCredentialsException("Bad Request Header Credentials");
        }

        return new PreAuthenticatedAuthenticationToken(authentication.getPrincipal(), null, new ArrayList<>());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(PreAuthenticatedAuthenticationToken.class);
    }
}
