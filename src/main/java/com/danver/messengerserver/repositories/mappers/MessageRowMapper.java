package com.danver.messengerserver.repositories.mappers;

import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.models.MessageData;
import com.danver.messengerserver.models.MessageDataType;
import com.danver.messengerserver.models.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MessageRowMapper implements RowMapper<Message> {
    @Override
    public Message mapRow(ResultSet rs, int rowNum) throws SQLException {
        Message message = new Message();
        message.setId(rs.getString("id"));
        message.setChatId(rs.getLong("chatId"));
        message.setAuthor(
                new User.Builder()
                        .id(rs.getLong("authorId"))
                        .name(rs.getString("name"))
                        .surname(rs.getString("surname"))
                        .avatarUrl(rs.getString("avatarUrl"))
                        .build()
        );
        message.setCreationTime(rs.getTimestamp("creationTime").toInstant());
        message.setData(new MessageData(MessageDataType.valueOf((String) rs.getObject("type")),
                rs.getObject("value")));
        return message;
    }
}
