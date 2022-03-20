package com.danver.messengerserver.repositories.implementations;

import com.danver.messengerserver.MessengerServerApplication;
import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.repositories.interfaces.MessageRepository;
import com.danver.messengerserver.repositories.mappers.MessageRowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
public class MessageRepositoryImpl implements MessageRepository {

    private static final Logger logger = LoggerFactory.getLogger(MessengerServerApplication.class.getName());
    private final JdbcTemplate jdbcTemplate;

    //MESSAGES TABLE FIELDS:
    //messageId, authorId, chatId, value(text), creationDate

    @Autowired
    public MessageRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Message> getMessages(long chatId, Instant from, Instant to) {
        logger.info("Getting messages for user with id: " + " for chat with id" + chatId);
        String query = "SELECT Messages.id, authorId, \"name\", surname, avatarUrl, chatId, type, value, creationTime " +
                "FROM Messages " +
                "INNER JOIN Users ON Messages.authorId = Users.id " +
                "WHERE chatId = ? AND creationTime >= ? AND creationTime <= ?";
        return this.jdbcTemplate.query(query, new MessageRowMapper(), chatId, from.atOffset(ZoneOffset.UTC),
                to.atOffset(ZoneOffset.UTC));
    }

    @Override
    public void createMessage(Message message) {
        logger.info("Writing message with id: " + message.getId() + " id to database");
        this.jdbcTemplate.update("INSERT INTO Messages VALUES (?, ?, ?, ?, CAST(? AS message_type), ?)",
                UUID.fromString(message.getId()),
                message.getChatId(),
                message.getAuthor().getId(),
                message.getCreationTime().atOffset(ZoneOffset.UTC),
                message.getData().getType().toString(),
                message.getData().getValue()
        );
    }
}
