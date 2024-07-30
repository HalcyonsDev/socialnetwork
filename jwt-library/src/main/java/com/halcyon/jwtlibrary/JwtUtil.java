package com.halcyon.jwtlibrary;

public class JwtUtil {
    private JwtUtil() {}

    public static JwtAuthentication getAuthentication(String subject) {
        final JwtAuthentication jwtAuthInfo = new JwtAuthentication();
        jwtAuthInfo.setEmail(subject);

        return jwtAuthInfo;
    }
}
