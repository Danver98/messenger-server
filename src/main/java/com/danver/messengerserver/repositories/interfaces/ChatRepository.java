package com.danver.messengerserver.repositories.interfaces;

import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.User;

import java.util.List;

public interface ChatRepository {

    Chat createChat(Chat chat);

    List<Chat> getChats(long userId);

    Chat getChat(long id);

    default List<User> getParticipants(long id) {
        return null;
    }

    List<Chat> getChatsWithParticipants(long userId);

    void updateChat(Chat chat);

    void deleteChat(long id);
}
