package com.danver.messengerserver.services.interfaces;

import com.danver.messengerserver.auth.AuthData;
import com.danver.messengerserver.models.User;

public interface MessengerAuthService {

    /**
     * Authenticates user when login
     * @param userAuthData user credentials
     * @param user user retrieved from db by login from *userAuthData*
     * @return new String generated token for authenticated user or Null if validation hasn't passed
     */
    String authenticateUser(AuthData userAuthData, User user);

    String createToken(String userId);

    String refreshToken(String userId, String token);
}
