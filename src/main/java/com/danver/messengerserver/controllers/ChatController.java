package com.danver.messengerserver.controllers;

import com.danver.messengerserver.MessengerServerApplication;
import com.danver.messengerserver.exceptions.AuthorizedAccessException;
import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.ChatPagingDTO;
import com.danver.messengerserver.models.ChatRequestDTO;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.services.interfaces.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/chats")
public class ChatController {
    /*
        TODO: We should take user from a context
        TODO: check rights!!!
     */

    private final ChatService chatService;

    private static final Logger logger = LoggerFactory.getLogger(MessengerServerApplication.class.getName());

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/")
    List<Chat> list(@RequestBody ChatPagingDTO dto) {
        // TODO: Check rights
        return chatService.getChats(dto);
    }

    @GetMapping("/{id}")
    Chat getChat(@RequestParam long userId, @PathVariable long id) {
        if (!chatService.userInChat(userId, id)) {
            throw new AuthorizedAccessException();
        }
        return chatService.getChat(id);
    }

    @PostMapping("/create")
    Chat createChat(@RequestBody Chat chat) {
        return chatService.createChat(chat);
    }

    @PutMapping("/{id}")
    void updateChat(@RequestParam long userId, @RequestBody Chat chat) {
        if (!chatService.userInChat(userId, chat.getId())) {
            throw new AuthorizedAccessException();
        }
        chatService.updateChat(chat);
    }

    @DeleteMapping("/{id}")
    void deleteChat(@RequestParam long userId, @PathVariable long id) {
        if (!chatService.userInChat(userId, id)) {
            throw new AuthorizedAccessException();
        }
        // TODO: publish event
        chatService.deleteChat(id);
    }

    @GetMapping("/{id}/participants")
    List<User> getListOfParticipants(@RequestParam long userId, @PathVariable long id) {
        if (!chatService.userInChat(userId, id)) {
            throw new AuthorizedAccessException();
        }
        return chatService.getParticipants(id);
    }

    @PostMapping("/add")
    ResponseEntity<?> addParticipants(@RequestBody ChatRequestDTO dto) {
        // if authorized
        this.chatService.addParticipants(dto.getChatId(), dto.getUsers());
        return ResponseEntity.ok().build();
    }
}
