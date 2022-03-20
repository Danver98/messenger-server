package com.danver.messengerserver.controllers;

import com.danver.messengerserver.exceptions.AuthorizedAccessException;
import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.services.interfaces.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/chats")
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/secret")
    String getSecret() {
        return "This is a secret chat!";
    }

    @GetMapping
    List<Chat> getChats(@PathVariable long userId) {
        return chatService.getChats(userId);
    }

    @GetMapping("/{id}")
    Chat getChat(@PathVariable long userId, @PathVariable long id) {
        if (chatService.getChats(userId).stream().noneMatch(chat -> chat.getId() == id)) {
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
        if (chatService.getChats(userId).stream().noneMatch(_chat -> _chat.getId() == chat.getId())) {
            throw new AuthorizedAccessException();
        }
        chatService.updateChat(chat);
    }

    @DeleteMapping("/{id}")
    void deleteChat(@PathVariable long userId, @PathVariable long id) {
        if (chatService.getChats(userId).stream().noneMatch(chat -> chat.getId() == id)) {
            throw new AuthorizedAccessException();
        }
        chatService.deleteChat(id);
    }

    @GetMapping("/{id}/participants")
    List<User> getListOfPrticipants(@PathVariable long userId, @PathVariable long id) {
        if (chatService.getChats(userId).stream().noneMatch(chat -> chat.getId() == id)) {
            throw new AuthorizedAccessException();
        }
        return chatService.getParticipants(id);
    }
}
