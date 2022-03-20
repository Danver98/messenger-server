package com.danver.messengerserver.repositories.mappers;

import com.danver.messengerserver.models.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User.Builder builder = new User.Builder();
        return builder.id(rs.getLong("id"))
                .name(rs.getString("name"))
                .surname(rs.getString("surname"))
                .email(rs.getString("email"))
                .salt(rs.getBytes("salt"))
                .password(rs.getString("passwordHash"))
                .avatarUrl(rs.getString("avatarUrl"))
                .build();
    }
}
