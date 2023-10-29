package com.danver.messengerserver.repositories.interfaces;

import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.models.MessageRequestDTO;

import java.time.Instant;
import java.util.List;

public interface MessageRepository {

    List<Message> getMessagesPaged(long chatId, Instant before, Instant after, String cursorMsgId, Integer count);

    List<Message> getMessages(MessageRequestDTO dto);

    void createMessage(Message message);
}
