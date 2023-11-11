package com.danver.messengerserver.services.interfaces;


import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.models.MessageRequestDTO;

import java.time.Instant;
import java.util.List;

public interface MessageService {


    List<Message> getMessages(MessageRequestDTO dto);

    /**
     *
     * @return message with updated absent fields
     */
    Message createMessage(Message message);

}
