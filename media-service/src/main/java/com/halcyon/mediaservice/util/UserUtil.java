package com.halcyon.mediaservice.util;

import com.halcyon.clients.user.UserResponse;
import com.halcyon.mediaservice.exception.UnverifiedUserException;
import com.halcyon.mediaservice.exception.UserIsBannedException;

public class UserUtil {
    private UserUtil() {}
    
    public static void isUserBanned(UserResponse userResponse, String message) {
        if (userResponse.isBanned()) {
            throw new UserIsBannedException(message);
        }
    }

    public static void isUserVerified(UserResponse userResponse, String message) {
        if (!userResponse.isVerified()) {
            throw new UnverifiedUserException(message);
        }
    }
}
