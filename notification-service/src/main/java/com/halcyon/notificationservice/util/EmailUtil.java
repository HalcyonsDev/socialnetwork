package com.halcyon.notificationservice.util;

public class EmailUtil {
    private static final String SUPPORT_TEAM = "The Social Network support Team";

    private EmailUtil() {}

    public static String getEmailVerificationMessage(String username, String host, String token) {
        return String.format(
                "Hello, %s!\n\nYour new account has been created. Please click the link below to verify your account. \n\n%s\n\n%s",
                username, getVerificationUrl(host, token), SUPPORT_TEAM
        );
    }

    public static String getResetPasswordMessage(String username, String host, String token) {
        return String.format(
                "Hello, %s! \n\nPlease click the link below to reset your password. \n\n %s\n\n%s",
                username, getResetPasswordUrl(host, token), SUPPORT_TEAM
        );
    }

    public static String getUserIsBanendMessage(String username) {
        return String.format(
                "Hello, %s!\n\nYou are banned for a large number of strikes from other users\n\n%s",
                username,
                SUPPORT_TEAM
        );
    }

    public static String getNewPostMessage(String ownerUsername, String targetUsername, long postId, String host) {
        return String.format(
                "Hello, %s!\n\nЕhe user under the username %s, to whom you are subscribed, has posted a new post!\n\n%s\n\n%s",
                targetUsername,
                ownerUsername,
                getPostUrl(host, postId),
                SUPPORT_TEAM
        );
    }


    private static String getVerificationUrl(String host, String token) {
        return host + "/api/v1/auth?token=" + token;
    }

    private static String getResetPasswordUrl(String host, String token) {
        return host + "/api/v1/auth/reset-password?token=" + token;
    }

    private static String getPostUrl(String host, long postId) {
        return host + "/api/v1/posts/" + postId;
    }
}
