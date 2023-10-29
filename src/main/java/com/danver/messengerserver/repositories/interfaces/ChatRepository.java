package com.danver.messengerserver.repositories.interfaces;

import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.User;

import java.time.Instant;
import java.util.List;

public interface ChatRepository {

    Chat createChat(Chat chat);

    List<Chat> getChats(long userId, Instant threshold, Long chatIdThreshold, int direction, Integer count);

    Chat getChat(long id);

    default List<User> getParticipants(long id) {
        return null;
    }

    List<Chat> getChatsWithParticipants(long userId);

    void updateChat(Chat chat);

    void deleteChat(long id);

    /**
     *
     * @return whether user is present in given chat
     */
    boolean userInChat(long userId, long chatId);

    void addParticipants(long chatId, long [] users);
}
