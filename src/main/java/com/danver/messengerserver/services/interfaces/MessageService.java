package com.danver.messengerserver.services.interfaces;


import com.danver.messengerserver.models.Message;

import java.time.Instant;
import java.util.List;

public interface MessageService {

    List<Message> getMessages(long chatId, Instant from, Instant to);

    /**
     *
     * @param message
     * @return message with updated absent fields
     */
    Message createMessage(Message message);

}
