package com.danver.messengerserver.repositories.interfaces;

import com.danver.messengerserver.models.User;
import org.springframework.lang.Nullable;

import java.util.List;

public interface UserRepository {

    //they may throw DataAccessException

    User createUser(User user);

    User getUser(long id);

    User getUserByEmail(String email);

    void updateUser(User user);

    void deleteUser(long id);

    List<User> searchUsers(@Nullable String name, @Nullable String surname);
}
