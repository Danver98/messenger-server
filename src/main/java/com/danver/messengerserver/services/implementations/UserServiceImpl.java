package com.danver.messengerserver.services.implementations;

import com.danver.messengerserver.MessengerServerApplication;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.models.UserRequestDTO;
import com.danver.messengerserver.repositories.interfaces.UserRepository;
import com.danver.messengerserver.services.interfaces.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(MessengerServerApplication.class.getName());

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return this.userRepository.createUser(user).flushPasswordAndSalt();
    }

    @Override
    @CachePut(cacheNames = "userDetails", key="#user.email")
    public void updateUser(User user) {
        this.userRepository.updateUser(user);
    }

    @Override
    @CacheEvict(cacheNames = "userDetails")
    public void deleteUser(long id) {
        this.userRepository.deleteUser(id);
    }

    @Override
    public List<User> list(UserRequestDTO dto) {
        String name, surname;
        if (dto.getFilter().getSearch() != null) {
            String[] params = dto.getFilter().getSearch().split(" ");
            if (params.length > 1) {

            }
        }
        return this.userRepository.list(dto);
    }

    // TODO: CacheErrorHandler() isn't invoked with 'sync' option enabled
    @Override
    @Cacheable(value = "userDetails", sync = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = this.getUserByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException(String.format("User %s is not found", email));
        }
        //return user; user.getEmail() as username
        return new org.springframework.security.core.userdetails.User(Long.toString(user.getId()), user.getPassword(),
                 true, true, true, true, new HashSet<>());
    }
}
