package com.danver.messengerserver.services.implementations;

import com.danver.messengerserver.MessengerServerApplication;
import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.models.MessageDataType;
import com.danver.messengerserver.models.MessageRequestDTO;
import com.danver.messengerserver.repositories.interfaces.MessageRepository;
import com.danver.messengerserver.services.interfaces.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessengerServerApplication.class.getName());

    private final MessageRepository messageRepository;

    @Autowired
    public MessageServiceImpl(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }


    @Override
    public List<Message> getMessages(MessageRequestDTO dto) {
        return messageRepository.getMessages(dto);
    }

    @Override
    public Message createMessage(Message message) {
        logger.info("Generating id for a new message");
        String id = UUID.randomUUID().toString();
        message.setId(id);
        message.setTime(Instant.now());
        if (message.getType() == null) {
            message.setType(Message.MessageType.CHAT);
        }
        if (message.getData() != null && message.getData().getType() == null) {
            message.getData().setType(MessageDataType.DEFAULT);
        }
        messageRepository.createMessage(message);
        return message;
    }
}
