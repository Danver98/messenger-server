package com.danver.messengerserver.controllers;

import com.danver.messengerserver.MessengerServerApplication;
import com.danver.messengerserver.exceptions.AuthorizedAccessException;
import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.services.interfaces.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/users/{userId}/chats")
public class ChatController {

    private final ChatService chatService;

    private static final Logger logger = LoggerFactory.getLogger(MessengerServerApplication.class.getName());

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    List<Chat> getChats(@PathVariable long userId,
                        @RequestParam(required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                        Instant prevLastChanged,
                        @RequestParam(required = false) Long prevChatId,
                        @RequestParam(required = false, defaultValue = "50") Integer count) {
        if (prevLastChanged != null)
            prevLastChanged = prevLastChanged.atOffset(ZoneOffset.UTC).toInstant();
        return chatService.getChats(userId, prevLastChanged, prevChatId, count);
    }

    @GetMapping("/{id}")
    Chat getChat(@PathVariable long userId, @PathVariable long id) {
        if (!chatService.userInChat(userId, id)) {
            throw new AuthorizedAccessException();
        }
        return chatService.getChat(id);
    }

    @PostMapping
    Chat createChat(@RequestBody Chat chat) {
        return chatService.createChat(chat);
    }

    @PutMapping("/{id}")
    void updateChat(@PathVariable long userId, @RequestBody Chat chat) {
        if (!chatService.userInChat(userId, chat.getId())) {
            throw new AuthorizedAccessException();
        }
        chatService.updateChat(chat);
    }

    @DeleteMapping("/{id}")
    void deleteChat(@PathVariable long userId, @PathVariable long id) {
        if (!chatService.userInChat(userId, id)) {
            throw new AuthorizedAccessException();
        }
        // TODO: publish event
        chatService.deleteChat(id);
    }

    @GetMapping("/{id}/participants")
    List<User> getListOfParticipants(@PathVariable long userId, @PathVariable long id) {
        if (!chatService.userInChat(userId, id)) {
            throw new AuthorizedAccessException();
        }
        return chatService.getParticipants(id);
    }
}
