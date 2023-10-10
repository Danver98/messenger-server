package com.danver.messengerserver.services.interfaces;


import com.danver.messengerserver.models.Message;

import java.time.Instant;
import java.util.List;

public interface MessageService {

    List<Message> getMessages(long chatId, Instant from, Instant to);

    List<Message> getMessagesPaged(long chatId, Instant before, Instant after, String cursorMsgId, Integer count);

    /**
     *
     * @return message with updated absent fields
     */
    Message createMessage(Message message);

}
