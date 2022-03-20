package com.danver.messengerserver.services.interfaces;

import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.User;

import java.util.List;

public interface ChatService {

     /**
      *
      * @param chat - chat object
      * @return newly created chat null if error occurred
      */
     Chat createChat(Chat chat);

     List<Chat> getChats(long userId);

     Chat getChat(long id);

     List<User> getParticipants(long id);

     void updateChat(Chat chat);

     void deleteChat(long id);
}
