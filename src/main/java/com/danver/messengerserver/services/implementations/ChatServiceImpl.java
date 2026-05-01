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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final PermissionService permissionService;

    @Autowired
    public ChatServiceImpl(ChatRepository chatRepository, PermissionService permissionService) {
        this.chatRepository = chatRepository;
        this.permissionService = permissionService;
    }

    @Override
    public Chat createChat(Chat chat) {
        Chat newChat = chatRepository.getOrCreate(chat);
        List<Long> participants = chat.getParticipants();
        if (participants != null && !participants.isEmpty()) {
            permissionService.grantAuthority(participants, newChat.getId(), ResourceType.CHAT.getValue(),
                    PermissionType.Chat.DEFAULT.getValue());
        }
        return newChat;
    }

    @Override
    @Transactional
    public Chat createChat(Chat chat, User author) {
        chat.setAuthorId(author.getId());
        Chat newChat = chatRepository.getOrCreate(chat);
        List<Long> participants = chat.getParticipants();
        if (participants == null || participants.isEmpty()) {
            return newChat;
        }
        participants.remove(author.getId());
        // Chat creator is assigned admin rights
        permissionService.grantAuthority(author.getId(), newChat.getId(), ResourceType.CHAT.getValue(),
                PermissionType.Chat.ADMIN.getValue());
        if (participants.isEmpty()) {
            return newChat;
        }
        // Other participants get default rights
        permissionService.grantAuthority(participants, newChat.getId(), ResourceType.CHAT.getValue(),
                PermissionType.Chat.DEFAULT.getValue());

        boolean canAddUsers = chat.isCanAddUsers();
        if (canAddUsers) {
            // grant authorities to add participants
            permissionService.grantAuthority(participants, chat.getId(), ResourceType.CHAT.getValue(),
                    PermissionType.Chat.User.ADD.getValue());
        }
        return newChat;
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
    @Transactional
    public void updateChat(Chat chat) {
        Chat oldChat = chatRepository.getChat(chat.getId(), null);
        chatRepository.updateChat(chat);
        boolean couldAddUsers = oldChat.isCanAddUsers();
        boolean canAddUsers = chat.isCanAddUsers();
        if (canAddUsers && !couldAddUsers) {
            // grant authorities to add participants
            List<Long> participants = chatRepository.getParticipants(chat.getId()).stream().map(User::getId).toList();
            permissionService.grantAuthority(participants, chat.getId(), ResourceType.CHAT.getValue(),
                     PermissionType.Chat.User.ADD.getValue());
        } else if (!canAddUsers && couldAddUsers) {
            // revoke permission
            List<Long> participants = chatRepository.getParticipants(chat.getId()).stream().map(User::getId).toList();
            permissionService.revokeAuthority(participants, chat.getId(), ResourceType.CHAT.getValue(),
                    PermissionType.Chat.User.ADD.getValue());
        }
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
    @Transactional
    public void deleteChat(long id) {
        // TODO: revoke permissions for users in this chat
        //permissionService.revokeAuthority();
        List<User> participants = chatRepository.getParticipants(id);
        long [] userIds = participants.stream().mapToLong(User::getId).toArray();
        permissionService.revokeAuthority(userIds, id, ResourceType.CHAT.getValue());
        chatRepository.deleteChat(id);
    }

    @Override
    public Chat exists(Long[] userIds) {
        return chatRepository.exists(userIds);
    }

    @Override
    @Transactional
    public void addParticipants(long chatId, long[] users) {
       this.chatRepository.addParticipants(chatId, users);
       permissionService.grantAuthority(users, chatId, ResourceType.CHAT.getValue(), PermissionType.Chat.DEFAULT.getValue());
    }

    @Override
    public Chat getAllUsersChat() {
        return this.chatRepository.getAllUsersChat();
    }

    @Override
    @Transactional
    public void deleteParticipants(long chatId, long[] userIds) {
        this.chatRepository.deleteParticipants(chatId, userIds);
        this.permissionService.revokeAuthority(userIds, chatId, ResourceType.CHAT.getValue());
    }
}
