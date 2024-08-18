package com.halcyon.userservice.util;

import com.halcyon.userservice.exception.UnverifiedUserException;
import com.halcyon.userservice.exception.BannedUserException;
import com.halcyon.userservice.model.User;

public class UserUtil {
    private UserUtil() {}
    
    public static void isUserBanned(User user, String message) {
        if (user.isBanned()) {
            throw new BannedUserException(message);
        }
    }

    public static void isUserVerified(User user, String message) {
        if (!user.isVerified()) {
            throw new UnverifiedUserException(message);
        }
    }
}
