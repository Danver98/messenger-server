package com.danver.messengerserver.repositories.implementations;

import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.repositories.interfaces.ChatRepository;
import com.danver.messengerserver.repositories.mappers.ChatRowMapper;
import com.danver.messengerserver.repositories.mappers.UserDTORowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ChatRepositoryImpl implements ChatRepository {

    private final JdbcTemplate jdbcTemplate;

    //Chats TABLE FIELDS:
    //id, name, avatarUrl, [participants], [messages]?
    // TODO: optimize queries

    @Autowired
    public ChatRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public Chat createChat(Chat chat) {
        String query = "INSERT INTO Chats (name, avatarUrl) VALUES (?, ?) RETURNING id";
        long id = jdbcTemplate.queryForObject(query, Long.class, chat.getName(), chat.getAvatarUrl());
        chat.setId(id);
        query = "INSERT Into UsersChats VALUES (?, ?)";
        List<Object[]> usersChats = chat.getParticipants().stream()
                .map(user -> new Object[]{user.getId(), chat.getId()}).collect(Collectors.toList());
        jdbcTemplate.batchUpdate(query, usersChats);
        return chat;
    }

    @Override
    @Transactional
    public List<Chat> getChats(long userId) {
        String query = "SELECT * FROM Chats INNER JOIN UsersChats ON UsersChats.chatId = Chats.id" +
                " WHERE UsersChats.userId = ?";
        return jdbcTemplate.query(query, new ChatRowMapper(), userId);
    }

    @Override
    @Transactional
    public List<Chat> getChatsWithParticipants(long userId) {
        String query = "SELECT * FROM Chats INNER JOIN UsersChats ON UsersChats.chatId = Chats.id" +
                " WHERE UsersChats.userId = ?";
        List<Chat> chats = jdbcTemplate.query(query, new ChatRowMapper(), userId);
        query = "SELECT id, name, surname, email, avatarUrl FROM Users INNER JOIN UsersChats ON UsersChats.userId = Users.id " +
                "WHERE UsersChats.chatId = ?";
        for (Chat chat : chats) {
            chat.setParticipants(jdbcTemplate.query(query, new UserDTORowMapper(), chat.getId()));
        }
        return chats;
    }

    @Override
    @Transactional
    public Chat getChat(long id) {
        String query = "SELECT * FROM Chats WHERE id = ?";
        Chat chat = jdbcTemplate.queryForObject(query, new ChatRowMapper(), id);
        query = "SELECT id, name, surname, email, avatarUrl FROM Users INNER JOIN UsersChats ON UsersChats.userId = Users.id " +
                "WHERE UsersChats.chatId = ?";
        chat.setParticipants(jdbcTemplate.query(query, new UserDTORowMapper(), id));
        return chat;
    }

    @Override
    @Transactional
    public void updateChat(Chat chat) {
        String query = "UPDATE Chats SET name = ?, avatarUrl = ? WHERE id = ?";
        jdbcTemplate.update(query, chat.getName(), chat.getAvatarUrl(), chat.getId());
        query = "DELETE FROM UsersChats WHERE chatId = ?";
        jdbcTemplate.update(query, chat.getId());
        query = "INSERT INTO UsersChats VALUES (?, ?)";
        List<Object[]> usersChats = chat.getParticipants().stream()
                .map(user -> new Object[]{user.getId(), chat.getId()}).collect(Collectors.toList());
        jdbcTemplate.batchUpdate(query, usersChats);
    }

    @Override
    @Transactional
    public void deleteChat(long id) {
        String query = "DELETE FROM Chats WHERE id = ?";
        jdbcTemplate.update(query, id);
        //Will it delete also from UsersChats table?
        //query = "DELETE FROM UsersChats WHERE chatId = ?";
        //jdbcTemplate.update(query, id);
    }
}
