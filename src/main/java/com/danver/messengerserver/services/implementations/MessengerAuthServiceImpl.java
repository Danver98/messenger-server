package com.danver.messengerserver.services.implementations;

import com.danver.messengerserver.MessengerServerApplication;
import com.danver.messengerserver.auth.AuthData;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.services.interfaces.MessengerAuthService;
import com.danver.messengerserver.utils.AuthUtil;
import com.danver.messengerserver.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

@Service
public class MessengerAuthServiceImpl implements MessengerAuthService {

    private static final Logger logger = LoggerFactory.getLogger(MessengerServerApplication.class.getName());

    private final AuthUtil authUtil;

    @Autowired
    public MessengerAuthServiceImpl(AuthUtil authUtil) {
        this.authUtil = authUtil;
    }

    @Override
    public String authenticateUser(AuthData authData, User user) {
        try {
            if (AuthUtil.hashPasswordWithSalt(authData.getPassword(), user.getSalt()).equals(user.getPasswordHash())) {
                return generateJWTToken(user);
            }
            logger.info("Attempt to login failed with wrong password for user " + user.getEmail());
            return null;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String createToken(String userId) {
        return null;
    }

    @Override
    public String refreshToken(String userId, String token) {
        return null;
    }

    private String generateJWTToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        // Now we just only put email
        claims.put(Constants.USER_JWT_EMAIL_KEY.getValue(), user.getEmail());
        return authUtil.generateJWTToken(claims, Long.toString(user.getId()));
    }
}
