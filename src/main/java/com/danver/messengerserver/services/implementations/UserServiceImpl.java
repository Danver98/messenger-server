package com.danver.messengerserver.services.implementations;

import com.danver.messengerserver.MessengerServerApplication;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.repositories.interfaces.UserRepository;
import com.danver.messengerserver.services.interfaces.UserService;
import com.danver.messengerserver.utils.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(MessengerServerApplication.class.getName());

    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getUser(long id) {
        return this.userRepository.getUser(id);
    }

    @Override
    public User getUserByEmail(String email) {
        return this.userRepository.getUserByEmail(email);
    }

    @Override
    public User createUser(User user) {
        //User has raw password
        if ((user.getSalt() == null)) {
            try {
                Object[] creds = AuthUtil.hashPassword(user.getPasswordHash());
                user.setPasswordHash((String) creds[0]);
                user.setSalt((byte[]) creds[1]);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                logger.info(this.getClass().getName() + " - Couldn't create a user: " + e.getMessage());
                return null;
            }
        }
        return this.userRepository.createUser(user).flushPasswordAndSalt();
    }

    @Override
    public void updateUser(User user) {
        this.userRepository.updateUser(user);
    }

    @Override
    public void deleteUser(long id) {
        this.userRepository.deleteUser(id);
    }

    @Override
    public List<User> searchUsers(@Nullable String name, @Nullable String surname) {
        return this.userRepository.searchUsers(name, surname);
    }
}
