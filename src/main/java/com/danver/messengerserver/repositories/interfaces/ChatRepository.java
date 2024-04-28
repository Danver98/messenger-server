package com.danver.messengerserver.repositories.interfaces;

import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.User;

import java.time.Instant;
import java.util.List;

public interface ChatRepository {

    Chat createChat(Chat chat);

    List<Chat> getChats(long userId, Instant threshold, Long chatIdThreshold, int direction, Integer count);

    default List<Chat> getChatsLight(long userId) {
        return null;
    }

    Chat getChat(long id);

    default List<User> getParticipants(long id) {
        return null;
    }

    List<Chat> getChatsWithParticipants(long userId);

    void updateChat(Chat chat);

    default void updateLastReadMsg(long chatId, long userId, String messageId) {

    };

    void deleteChat(long id);

    /**
     * Check whether chat with given users exists
     * @return existing chat or null
     */
    Chat exists(Long[] userIds);

    /**
     * Check whether chat with given params exists (id, participants ids)
     * @return newly created chat or existing one
     */
    Chat getOrCreate(Chat chat);

    void addParticipants(long chatId, long [] users);
}
