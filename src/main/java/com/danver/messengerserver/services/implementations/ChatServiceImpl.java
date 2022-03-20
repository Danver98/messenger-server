package com.danver.messengerserver.services.implementations;

import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.repositories.interfaces.ChatRepository;
import com.danver.messengerserver.services.interfaces.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;

    @Autowired
    public ChatServiceImpl(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Override
    public Chat createChat(Chat chat) {
        chatRepository.createChat(chat);
        return chat;
    }

    @Override
    public List<Chat> getChats(long userId) {
        //return chatRepository.getChatsWithParticipants(userId);
        return chatRepository.getChats(userId);
    }

    @Override
    public Chat getChat(long id) {
        return chatRepository.getChat(id);
    }

    @Override
    public List<User> getParticipants(long id) {
        return chatRepository.getParticipants(id);
    }

    @Override
    public void updateChat(Chat chat) {
        chatRepository.updateChat(chat);
    }

    @Override
    public void deleteChat(long id) {
        chatRepository.deleteChat(id);
    }
}
