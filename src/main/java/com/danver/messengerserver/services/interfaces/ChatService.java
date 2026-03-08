package com.danver.messengerserver.services.interfaces;

import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.ChatPagingDTO;
import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.models.User;

import java.time.Instant;
import java.util.List;

public interface ChatService {

     /**
      *
      * @param chat - chat object
      * @return newly created chat, null if error occurred
      */
     Chat createChat(Chat chat);

     List<Chat> getChats(ChatPagingDTO dto);

     default List<Chat> getChatsLight(ChatPagingDTO dto) {
          return this.getChats(dto);
     }

     Chat getChat(long id, long userId);

     List<User> getParticipants(long id);

     void updateChat(Chat chat);

     default void updateLastReadMsg(long chatId, long userId, String messageId) {

     }

     default void updateLastReadMsgForDeleted(List<Message> messages) {

     };

     void deleteChat(long id);


     /**
      * Check whether chat with given users exists
      * @return existing chat or null
      */
     Chat exists(Long[] userIds);

     void addParticipants(long chatId, long[] users);

     Chat getAllUsersChat();
}
