package com.danver.messengerserver.repositories.mappers;

import com.danver.messengerserver.models.Chat;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public class ChatRowMapper implements RowMapper<Chat> {
    @Override
    public Chat mapRow(ResultSet rs, int rowNum) throws SQLException {
        Chat chat = new Chat();
        chat.setId(rs.getLong("id"));
        chat.setName(rs.getString("name"));
        chat.setAvatarUrl(rs.getString("avatarUrl"));
        chat.setLastChanged(rs.getObject("lastChanged", Instant.class));
        chat.setPrivate(rs.getBoolean("private"));
        return chat;
    }
}
