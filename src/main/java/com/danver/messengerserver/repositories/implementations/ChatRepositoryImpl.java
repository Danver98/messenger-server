package com.danver.messengerserver.repositories.implementations;

import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.util.Direction;
import com.danver.messengerserver.repositories.interfaces.ChatRepository;
import com.danver.messengerserver.repositories.mappers.ChatRowMapper;
import com.danver.messengerserver.repositories.mappers.UserDTORowMapper;
import com.danver.messengerserver.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
public class ChatRepositoryImpl implements ChatRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private record ChatPagingDtoProperTime(Long userId, OffsetDateTime time, Long chatId, Direction direction, Integer count) {
    }

    //Chats TABLE FIELDS:
    //id, name, avatarUrl, lastChanged, [participants], [messages]?
    // TODO: optimize queries

    @Autowired
    public ChatRepositoryImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    @Transactional
    public Chat createChat(Chat chat) {
        String query = "INSERT INTO Chats (name, avatarUrl, private) VALUES (?, ?, ?) RETURNING id";
        long id = jdbcTemplate.queryForObject(query, Long.class, chat.getName(), chat.getAvatarUrl(), chat.isPrivate());
        chat.setId(id);
        query = "INSERT Into UsersChats VALUES (?, ?)";
        List<Object[]> usersChats = chat.getParticipants().stream()
                .map(user -> new Object[]{user.getId(), chat.getId()}).collect(Collectors.toList());
        jdbcTemplate.batchUpdate(query, usersChats);
        return chat;
    }

    @Override
    @Transactional
    public List<Chat> getChats(long userId, Instant threshold, Long chatIdThreshold, int direction, Integer count) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("userId", userId);
        namedParameters.addValue("threshold", threshold);
        namedParameters.addValue("chatIdThreshold", chatIdThreshold);
        namedParameters.addValue("direction", direction == Direction.FUTURE.ordinal() ? '>' : '<');
        char compareSign =  direction == Direction.FUTURE.ordinal() ? '>' : '<';
        String order = direction == Direction.FUTURE.ordinal() ? "ASC" : "DESC";
        namedParameters.addValue("orderDirection", direction == Direction.FUTURE.ordinal() ? "ASC" : "DESC");
        namedParameters.addValue("count", count == null ? Integer.parseInt(Constants.CHATS_ONE_PAGE_COUNT) : count);
        Direction dir = direction == 0 ? Direction.FUTURE : Direction.PAST;
        ChatPagingDtoProperTime dto = new ChatPagingDtoProperTime(userId,
                threshold == null ? null : threshold.atOffset(ZoneOffset.UTC),
                chatIdThreshold, dir, count);
        String query = String.format("""
                SELECT
                    c.id,
                    c.name,
                    c.avatarUrl,
                    c.lastChanged,
                    c.private
                FROM
                    Chats c
                INNER JOIN
                    UsersChats uc
                ON
                    uc.chatId = c.id
                WHERE 
                    uc.userId = :userId
                AND
                    CASE
                        WHEN :time::timestamp with time zone IS NULL
                            THEN TRUE
                        ELSE
                            CASE
                                    WHEN :chatId IS NULL
                                        THEN c.lastChanged %c :time::timestamp with time zone
                                    ELSE
                                        (c.lastChanged, c.id) %c (:time::timestamp with time zone, :chatId)
                            END
                    END
                ORDER BY
                    c.lastChanged %s,
                    c.id
                FETCH FIRST :count ROWS ONLY
                """, compareSign, compareSign, order);
        return namedParameterJdbcTemplate.query(query, new BeanPropertySqlParameterSource(dto), new ChatRowMapper());
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
        if (chat != null)
            chat.setParticipants(jdbcTemplate.query(query, new UserDTORowMapper(), id));
        return chat;
    }

    @Override
    @Transactional
    public void updateChat(Chat chat) {
        String query = "UPDATE Chats SET name = ?, avatarUrl = ?, private = ? WHERE id = ?";
        jdbcTemplate.update(query, chat.getName(), chat.getAvatarUrl(), chat.getId(), chat.isPrivate());
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
    }

    @Override
    public boolean userInChat(long userId, long chatId) {
        String query = """
            SELECT EXISTS (
                SELECT
                    1
                FROM
                    UsersChats uc
                WHERE
                    uc.userId = ?
                    AND uc.chatId = ?
            )
            
        """;
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(query, Boolean.class, userId, chatId));
    }

    @Override
    public void addParticipants(long chatId, long[] users) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("chatId", chatId);
        namedParameters.addValue("users", users);
        String query= """
            insert into
                "UsersChats" (chatId, userId)
            select
                :chatId, unnest(:users)
        """;
        jdbcTemplate.update(query, namedParameters);
    }
}
