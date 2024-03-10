package com.danver.messengerserver.repositories.implementations;

import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.util.Direction;
import com.danver.messengerserver.repositories.interfaces.ChatRepository;
import com.danver.messengerserver.repositories.mappers.ChatListRowMapper;
import com.danver.messengerserver.repositories.mappers.ChatRowMapper;
import com.danver.messengerserver.utils.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class ChatRepositoryImpl implements ChatRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private record ChatPagingDtoProperTime(Long userId, OffsetDateTime time, Long chatId, Direction direction, Integer count) {
    }

    private final RedisTemplate<String, ?> redisTemplate;

    //Chats TABLE FIELDS:
    //id, name, avatarUrl, lastChanged, [participants], [messages]?
    // TODO: optimize queries

    @Autowired
    public ChatRepositoryImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate, RedisTemplate<String, ?> redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public Chat createChat(Chat chat) {
        String query = """
        insert into
            Chats (name, "avatarUrl", private, draft)
        values
            (:name, :avatar, :private, true)
        on conflict
            do nothing
        returning id
        """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("name", chat.getName(), Types.VARCHAR);
        params.addValue("avatar", chat.getAvatarUrl(), Types.VARCHAR);
        params.addValue("private", chat.isPrivate(), Types.BOOLEAN);
        Long id = namedParameterJdbcTemplate.queryForObject(query, params, Long.class);
        if (id == null) {
            return null;
        }
        chat.setId(id);
        if (chat.getParticipants() != null) {
            Long [] userIds = chat.getParticipants().toArray(new Long[0]);
            params.addValue("chatId", id);
            params.addValue("users", userIds, Types.ARRAY);
            query= """
            insert into
                UsersChats (chatId, userId)
            select
                :chatId, unnest(:users)
        """;
            namedParameterJdbcTemplate.update(query, params);
        }
        return this.getChat(id);
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
                with last_msgs as (
                    select
                        m."chatId",
                        m.id,
                        m.type,
                        m.value_type,
                        m.value,
                        m."lastChanged",
                        author.id "authorId",
                        author.name "authorName",
                        author.surname "authorSurname",
                        author."avatarUrl" "authorAvatarUrl"
                    from
                        Messages m
                    join (
                        select
                            m."chatId",
                            max(m."lastChanged") "lastChanged"
                        from
                            Messages m
                        group by
                            m."chatId"
                    ) max_data
                        on m."chatId" = max_data."chatId"
                        and m."lastChanged" = max_data."lastChanged"
                    join Users author
                        on m."authorId" = author.id
                ), private_chats as (
                    select
                        array_agg(c.id) "ids"
                    from
                        Chats c
                    join
                        UsersChats uc
                    on
                        uc.chatId = c.id
                    WHERE
                        c.private is true
                        and uc.userId = :userId
                ), private_chats_names as (
                    select
                        uc.chatId "id",
                        u.name || ' ' || u.surname "name"
                    from
                        UsersChats uc
                    join Users u
                        on uc.userId = u.id
                        and uc.userId is distinct from :userId
                    where
                        uc.chatId = any((select ids from private_chats)::bigint[])
                )
                select
                    c.id,
                    case
                        when c.private is true
                            then pcn.name
                        else
                            c.name
                    end "name",
                    c."avatarUrl",
                    c."lastChanged",
                    c.private,
                    c.draft,
                    -- last message in chat
                    last_msg.id "lastMsg.id",
                    last_msg."chatId" "lastMsg.chatId",
                    last_msg.type "lastMsg.type",
                    last_msg.value_type "lastMsg.valueType",
                    last_msg.value "lastMsg.value",
                    last_msg."lastChanged" "lastMsg.lastChanged",
                    last_msg."authorId" "lastMsg.authorId",
                    last_msg."authorName" "lastMsg.authorName",
                    last_msg."authorSurname" "lastMsg.authorSurname",
                    last_msg."authorAvatarUrl" "lastMsg.authorAvatarUrl"
                from
                    Chats c
                join
                    UsersChats uc
                on
                    uc.chatId = c.id
                left join private_chats_names pcn
                    on c.id = pcn.id
                    and c.private is true
                LEFT JOIN LATERAL (
                    select
                        *
                    from
                        last_msgs m
                    where
                        m."chatId" = c.id
                ) last_msg
                ON TRUE
                WHERE
                    uc.userId = :userId
                    and c.draft is not true
                AND
                    CASE
                        WHEN :time::timestamp with time zone IS NULL
                            THEN TRUE
                        ELSE
                            CASE
                                    WHEN :chatId IS NULL
                                        THEN c."lastChanged" %c :time::timestamp with time zone
                                    ELSE
                                        (c."lastChanged", c.id) %c (:time::timestamp with time zone, :chatId)
                            END
                    END
                ORDER BY
                    c."lastChanged" %s,
                    c.id
                FETCH FIRST :count ROWS ONLY
                """, compareSign, compareSign, order);
        return namedParameterJdbcTemplate.query(query, new BeanPropertySqlParameterSource(dto), new ChatListRowMapper());
    }

    @Override
    @Transactional
    public List<Chat> getChatsWithParticipants(long userId) {
        return null;
    }

    @Override
    @Transactional
    public Chat getChat(long id) {
        String query = """
            with participants as (
                select
                    c.id "chatId",
                    array_agg(distinct uc.userid) "participants",
                    array_agg(u.name || ' ' || u.surname) "user_names"
                from
                    Chats c
                join UsersChats uc
                    on c.id = :chatId
                    and c.id = uc.chatId
                    and c.private is true
                join Users u
                    on uc.userId = u.id
                group by
                    c.id
            )
            select
                c.*,
                case c."private"
                    when true
                        then pts."participants"
                    else
                        null::bigint[]
                end "participants",
                array_to_string(pts."user_names", '|') "user_names"
            FROM
                Chats c
            left join participants pts
                on c."id" = pts."chatId"
            WHERE
                id = :chatId
        """;
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("chatId", id, Types.BIGINT);
        try {
            return namedParameterJdbcTemplate.queryForObject(query, namedParameters, new ChatRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    @Transactional
    public void updateChat(Chat chat) {
        String query = "UPDATE Chats SET name = ?, \"avatarUrl\" = ?, private = ? WHERE id = ?";
        jdbcTemplate.update(query, chat.getName(), chat.getAvatarUrl(), chat.getId(), chat.isPrivate());
        query = "DELETE FROM UsersChats WHERE \"chatId\" = ?";
        jdbcTemplate.update(query, chat.getId());
/*        query = "INSERT INTO UsersChats VALUES (?, ?)";
        List<Object[]> usersChats = chat.getParticipants().stream()
                .map(user -> new Object[]{user.getId(), chat.getId()}).collect(Collectors.toList());
        jdbcTemplate.batchUpdate(query, usersChats);*/
    }

    @Override
    @Transactional
    public void deleteChat(long id) {
        String query = "DELETE FROM Chats WHERE id = ?";
        jdbcTemplate.update(query, id);
    }

    @Override
    public Chat exists(Long[] userIds) {
        if (userIds.length < 2) return null;
        if (Objects.equals(userIds[0], userIds[1])) return null;
        String query = """
            with common_chats as (
                select
                    chatId
                from
                    UsersChats
                where
                    userId is not distinct from :first
                
                intersect
                
                select
                    chatId
                from
                    UsersChats
                where
                    userId is not distinct from :second
            )
            select
                1
            from
                Chats c
            where
                c.private is true
                and c.id = any ((select array_agg(chatId) from common_chats )::bigint[])
            limit
                1
        """;
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("first", userIds[0], Types.BIGINT);
        namedParameters.addValue("second", userIds[1], Types.BIGINT);
        Long chat = namedParameterJdbcTemplate.queryForObject(query, namedParameters, Long.class);
        if (chat != null) {
            return this.getChat(chat);
        }
        return null;
    }

    @Override
    public Chat getOrCreate(Chat chat) {
        Chat existing = this.getChat(chat.getId());
        if (existing != null) return existing;
        return this.createChat(chat);
    }

    @Override
    public void addParticipants(long chatId, long[] users) {
        addParticipantsToRedisAsync(chatId, users);
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

    @Async
    public void addParticipantsToRedisAsync(long chatId, long[] users) {
        log.info("Executing addParticipantsToRedisAsync method - " + Thread.currentThread().getName());
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        List<String> values = hashOps.multiGet(Constants.REDIS_USERS_PERMISSIONS, Arrays.stream(users).mapToObj(String::valueOf).toList());
        int i = 0;
        Map<String, String> userChats = new HashMap<>();
        for (long userId: users) {
            List<String> chatList = new ArrayList<>(List.of(values.get(i++).split(",")));
            chatList.add(Long.toString(chatId));
            userChats.put(Long.toString(userId), chatList.toString());
        }
        hashOps.putAll(Constants.REDIS_USERS_PERMISSIONS, userChats);
    }

    private List<Long> getChats(Long userId){
        return jdbcTemplate.queryForList("""
                select distinct
                    "chatId"
                from
                    "usersChats"
                where
                    "userid" = ?
        """, Long.class, userId);
    }
}
