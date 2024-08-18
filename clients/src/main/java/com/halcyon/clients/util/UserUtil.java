package com.halcyon.clients.util;

import com.halcyon.clients.exception.BannedUserException;
import com.halcyon.clients.exception.UnverifiedUserException;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserResponse;

public class UserUtil {
    private UserUtil() {}

    public static void isUserBanned(UserResponse userResponse, String message) {
        if (userResponse.isBanned()) {
            throw new BannedUserException(message);
        }
    }

    public static void isUserBanned(PrivateUserResponse privateUserResponse, String message) {
        if (privateUserResponse.isBanned()) {
            throw new BannedUserException(message);
        }
    }

    public static void isUserVerified(UserResponse userResponse, String message) {
        if (!userResponse.isVerified()) {
            throw new UnverifiedUserException(message);
        }
    }

    public static void isUserVerified(PrivateUserResponse privateUserResponse, String message) {
        if (!privateUserResponse.isVerified()) {
            throw new UnverifiedUserException(message);
        }
    }
}
