package com.danver.messengerserver.services.implementations;

import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.ChatPagingDTO;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.repositories.interfaces.ChatRepository;
import com.danver.messengerserver.services.interfaces.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;

    @Autowired
    public ChatServiceImpl(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Override
    public Chat createChat(Chat chat) {
        return chatRepository.getOrCreate(chat);
    }

    @Override
    public List<Chat> getChats(ChatPagingDTO dto) {
        //return chatRepository.getChatsWithParticipants(userId);

        return chatRepository.getChats(dto.getUserId(), dto.getTime(), dto.getChatId(),
                dto.getDirection().ordinal(), dto.getCount());
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

    @Override
    public boolean userInChat(long userId, long chatId) {
        return chatRepository.userInChat(userId, chatId);
    }

    @Override
    public Chat exists(Long[] userIds) {
        return chatRepository.exists(userIds);
    }

    @Override
    public void addParticipants(long chatId, long[] users) {
       this.chatRepository.addParticipants(chatId, users);
    }
}
