package com.danver.messengerserver.repositories.interfaces;

import com.danver.messengerserver.models.Message;

import java.time.Instant;
import java.util.List;

public interface MessageRepository {

    List<Message> getMessages(long chatId, Instant from, Instant to);

    List<Message> getMessagesPaged(long chatId, Instant before, Instant after, String cursorMsgId, Integer count);

    void createMessage(Message message);
}
