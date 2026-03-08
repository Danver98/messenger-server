package com.danver.messengerserver.repositories.interfaces;

import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.models.MessageRequestDTO;

import java.time.Instant;
import java.util.List;

public interface MessageRepository {

    List<Message> getMessages(MessageRequestDTO dto);

    void createMessage(Message message);

    void deleteMessages(List<Message> messages);
}
