package com.danver.messengerserver.repositories.implementations;

import com.danver.messengerserver.MessengerServerApplication;
import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.models.MessageRequestDTO;
import com.danver.messengerserver.models.util.Direction;
import com.danver.messengerserver.repositories.interfaces.MessageRepository;
import com.danver.messengerserver.repositories.mappers.MessageRowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class MessageRepositoryImpl implements MessageRepository {

    private record MessageRequestDtoProperTime(
            Long chatId, Long userId, OffsetDateTime time, String messageId, Direction direction, Integer count) {

    }

    private static final Logger logger = LoggerFactory.getLogger(MessengerServerApplication.class.getName());
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MessageRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Message> getMessages(MessageRequestDTO dto) {
        if (dto.getTime() == null) {
            // User's just opened chat, first fetch's needed
            return this.getMessagesFirst(dto);
        }
        logger.info("Getting messages for user with id: " + " for chat with id" + dto.getChatId() + "using paging");
        MessageRequestDtoProperTime dtoProper = new MessageRequestDtoProperTime(
                dto.getChatId(), dto.getUserId(), dto.getTime() == null ? null : dto.getTime().atOffset(ZoneOffset.UTC),
                dto.getMessageId(), dto.getDirection(), dto.getCount());
        char compareSign = dto.getDirection() == Direction.FUTURE ? '>' : '<';
        String order = dto.getDirection() == Direction.FUTURE ? "ASC" : "DESC";
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(Objects.requireNonNull(this.jdbcTemplate.getDataSource()));
        String query = String.format("""
                SELECT
                    "Messages".id,
                    "authorId",
                    "name",
                    surname,
                    "avatarUrl",
                    "chatId",
                    type,
                    value,
                    value_type,
                    "lastChanged",
                    true::boolean "read"
                FROM
                    "Messages"
                INNER JOIN "Users"
                    ON "Messages"."authorId" = "Users".id
                WHERE
                    "chatId" = :chatId
                    AND CASE
                            WHEN :time::timestamp with time zone IS NULL
                                THEN TRUE
                            ELSE
                                CASE
                                    WHEN :messageId::uuid IS NULL
                                        THEN "lastChanged" %c :time::timestamp with time zone
                                    ELSE
                                        ("lastChanged", "Messages".id) %c (:time::timestamp with time zone, :messageId::uuid)
                                END
                        END
                ORDER BY
                    "lastChanged" %s
                FETCH FIRST :count ROWS ONLY
                """, compareSign, compareSign, order);

        return namedParameterJdbcTemplate.query(query, new BeanPropertySqlParameterSource(dtoProper), new MessageRowMapper());
    }

    public List<Message> getMessagesFirst(MessageRequestDTO dto) {
        logger.info("Getting messages for user with id: " + dto.getUserId() + " for chat with id" + dto.getChatId() + "using paging");
        MessageRequestDtoProperTime dtoProper = new MessageRequestDtoProperTime(
                dto.getChatId(), dto.getUserId(), dto.getTime() == null ? null : dto.getTime().atOffset(ZoneOffset.UTC),
                dto.getMessageId(), dto.getDirection(), dto.getCount());
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(Objects.requireNonNull(this.jdbcTemplate.getDataSource()));
        String query = """
                with last_read_msg as (
                    select
                        "id",
                        "lastChanged"
                    from
                        "Messages"
                    where
                        "id" = (
                            select
                                "lastReadMsg"
                            from
                                "UsersChats"
                            where
                                "userId" = :userId
                                and "chatId" = :chatId
                        )
                ),
                -- First fetch when opening chat (:time is null)
                -- 1 step: get messages from "lastReadMsg" to latest one;
                -- 2 step: get :count messages before "lastReadMsg".
                unread_msgs as (
                    select
                        "Messages".id,
                        "authorId",
                        "name",
                        surname,
                        "avatarUrl",
                        "chatId",
                        type,
                        value,
                        value_type,
                        "lastChanged",
                        false::boolean "read"
                    from
                        "Messages"
                    inner join "Users"
                        ON "Messages"."authorId" = "Users".id
                    where
                        "chatId" = :chatId
                        and :time::timestamp with time zone IS NULL
                        and "Messages"."id" is distinct from (
                            select
                                "id"
                            from
                                last_read_msg
                        )
                        and "lastChanged" >= coalesce((
                            select
                                "lastChanged"
                            from
                                last_read_msg
                        ), to_timestamp(0))
                ),
                read_msgs as (
                    select
                        "Messages".id,
                        "authorId",
                        "name",
                        surname,
                        "avatarUrl",
                        "chatId",
                        type,
                        value,
                        value_type,
                        "lastChanged",
                        true::boolean "read"
                    from
                        "Messages"
                    inner join "Users"
                        on "Messages"."authorId" = "Users".id
                    where
                        "chatId" = :chatId
                        and "Messages"."id" is not distinct from (
                            select
                                "id"
                            from
                                last_read_msg
                        )
                        or "lastChanged" < coalesce((
                            select
                                "lastChanged"
                            from
                                last_read_msg
                        ), to_timestamp(0))
                    order by
                        "lastChanged" desc
                    fetch first :count rows only
                )
                select
                    *
                from
                    unread_msgs
                
                union

                select
                    *
                from
                    read_msgs
                order by
                    "lastChanged" desc
                """;

        return namedParameterJdbcTemplate.query(query, new BeanPropertySqlParameterSource(dtoProper), new MessageRowMapper());
    }

    @Override
    public void createMessage(Message message) {
        if (message == null) return;
        logger.info("Writing message with id: " + message.getId() + " id to database");
        String query = """
                    insert into
                        Messages (id, "chatId", "authorId", "lastChanged", value, type, value_type)
                    values
                        (?, ?, ?, ?, ?, ?, ?)
                """;
        this.jdbcTemplate.update(query,
                UUID.fromString(message.getId()),
                message.getChatId(),
                message.getAuthor().getId(),
                message.getTime().atOffset(ZoneOffset.UTC),
                message.getData().getValue(), //carefully!
                message.getType().ordinal() + 1,
                message.getData().getType().ordinal() + 1
        );
    }
}
