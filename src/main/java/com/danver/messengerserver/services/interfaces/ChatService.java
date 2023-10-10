package com.danver.messengerserver.services.interfaces;

import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.User;

import java.time.Instant;
import java.util.List;

public interface ChatService {

     /**
      *
      * @param chat - chat object
      * @return newly created chat null if error occurred
      */
     Chat createChat(Chat chat);

     /**
      *
      * @param prevLastChanged time since we start selecting chats by the time they were last updated;
      *                        if not specified, method returns all chats for given user
      * @param prevChatId id of the last chat from previous selection by client
      * @param count number of records to give back
      */
     List<Chat> getChats(long userId, Instant prevLastChanged, Long prevChatId, Integer count);

     Chat getChat(long id);

     List<User> getParticipants(long id);

     void updateChat(Chat chat);

     void deleteChat(long id);

     /**
      *
      * @return whether user is present in given chat
      */
     boolean userInChat(long userId, long chatId);
}
