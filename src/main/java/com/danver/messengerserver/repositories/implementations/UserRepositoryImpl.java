package com.danver.messengerserver.repositories.implementations;

import com.danver.messengerserver.models.User;
import com.danver.messengerserver.repositories.interfaces.UserRepository;
import com.danver.messengerserver.repositories.mappers.UserDTORowMapper;
import com.danver.messengerserver.repositories.mappers.UserRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final JdbcTemplate jdbcTemplate;

    //USERS TABLE FIELDS:
    //id, name, surname, email, salt, passwordHash, avatarUrl

    @Autowired
    public UserRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User createUser(User user) {
        String query = "INSERT INTO Users (name, surname, email, salt, passwordHash, avatarUrl)" +
                " VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        long id = jdbcTemplate.queryForObject(query, Long.class, user.getName(), user.getSurname(),
                user.getEmail(), user.getSalt(), user.getPasswordHash(), user.getAvatarUrl());
        user.setId(id);
        return user;
    }

    @Override
    public User getUser(long id) {
        String query = "SELECT id, name, surname, email, avatarUrl FROM Users WHERE id = ?";
        return jdbcTemplate.queryForObject(query, new UserDTORowMapper(), id);
    }

    @Override
    public User getUserByEmail(String email) {
        String query = "SELECT * FROM Users WHERE email = ?";
        // We use UserRowMapper here to process password and salt
        return jdbcTemplate.queryForObject(query, new UserRowMapper(), email);
    }

    @Override
    public void updateUser(User user) {
        String query = "UPDATE Users SET name = ?, surname = ?, email = ?, $PASSWORD " +
                "avatarUrl = ? WHERE id = ?";
        String withPassword = "salt = ?, passwordHash = ?,";
        if (user.getSalt() == null || user.getPasswordHash() == null) {
            query = query.replace("$PASSWORD", "");
        } else {
            query = query.replace("$PASSWORD", withPassword);
        }
        jdbcTemplate.update(query, user.getName(), user.getSurname(), user.getEmail(), user.getSalt(),
                user.getPasswordHash(), user.getAvatarUrl(), user.getId());
    }

    @Override
    public void deleteUser(long id) {
        String query = "DELETE FROM Users WHERE id = ?";
        jdbcTemplate.update(query, id);
    }

    @Override
    public List<User> searchUsers(String name, String surname) {
        String query;
        if (name != null && surname != null) {
            query = "SELECT id, name, surname, email, avatarUrl FROM Users WHERE name = ? AND surname = ?";
            return this.jdbcTemplate.query(query, new UserDTORowMapper(), name, surname);
        } else if (name == null) {
            query = "SELECT id, name, surname, email, avatarUrl FROM Users WHERE surname = ?";
            return this.jdbcTemplate.query(query, new UserDTORowMapper(), surname);
        } else {
            query = "SELECT id, name, surname, email, avatarUrl FROM Users WHERE name = ?";
            return this.jdbcTemplate.query(query, new UserDTORowMapper(), name);
        }
    }
}
