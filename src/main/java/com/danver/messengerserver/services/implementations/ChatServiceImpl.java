package com.danver.messengerserver.services.implementations;

import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.ChatPagingDTO;
import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.repositories.interfaces.ChatRepository;
import com.danver.messengerserver.services.interfaces.ChatService;
import com.danver.messengerserver.services.permission.PermissionService;
import com.danver.messengerserver.services.permission.PermissionType;
import com.danver.messengerserver.services.permission.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.util.List;
import java.util.Objects;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final PermissionService permissionService;

    private final JdbcTemplate jdbcTemplate;

    private final CharacterEncodingFilter characterEncodingFilter;

    @Autowired
    public ChatServiceImpl(ChatRepository chatRepository, PermissionService permissionService,
                           JdbcTemplate jdbcTemplate,
                           CharacterEncodingFilter characterEncodingFilter) {
        this.chatRepository = chatRepository;
        this.permissionService = permissionService;
        this.jdbcTemplate = jdbcTemplate;
        this.characterEncodingFilter = characterEncodingFilter;
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
    public List<Chat> getChatsLight(ChatPagingDTO dto) {
        return chatRepository.getChatsLight(dto.getUserId());
    }

    @Override
    public Chat getChat(long id, long userId) {
        return chatRepository.getChat(id, userId);
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
    public void updateLastReadMsg(long chatId, long userId, String messageId) {
        chatRepository.updateLastReadMsg(chatId, userId, messageId);
    }

    @Override
    public void updateLastReadMsgForDeleted(List<Message> messages) {
        chatRepository.updateLastReadMsgForDeleted(messages);
    }

    @Override
    public void deleteChat(long id) {
        chatRepository.deleteChat(id);
    }

    @Override
    public Chat exists(Long[] userIds) {
        return chatRepository.exists(userIds);
    }

    @Override
    public void addParticipants(long chatId, long[] users) {
       this.chatRepository.addParticipants(chatId, users);
       for (long user: users) {
           permissionService.grantAuthority(user, chatId, ResourceType.CHAT.getValue(), PermissionType.Chat.DEFAULT.getValue());
       }
    }

    @Override
    public Chat getAllUsersChat() {
        return this.chatRepository.getAllUsersChat();
    }
}
