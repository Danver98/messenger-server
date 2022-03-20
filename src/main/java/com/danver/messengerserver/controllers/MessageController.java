package com.danver.messengerserver.controllers;

import com.danver.messengerserver.exceptions.AuthorizedAccessException;
import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.services.interfaces.ChatService;
import com.danver.messengerserver.services.interfaces.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/chats/{chatId}/messages")
public class MessageController {

    private final MessageService messageService;
    private final ChatService chatService;

    @Autowired
    public MessageController(MessageService messageService, ChatService chatService) {
        this.messageService = messageService;
        this.chatService = chatService;
    }

    @GetMapping("/secret")
    String getSecret() {
        return "This is a secret message!";
    }

    @GetMapping
    ResponseEntity<List<Message>> getMessagesByDate(@PathVariable long chatId, @PathVariable long userId,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                    @RequestParam(required = false) Integer lastCount) {
        if (chatService.getChats(userId).stream().noneMatch(chat -> chat.getId() == chatId)) {
            throw new AuthorizedAccessException();
        }
        Instant fromInst, toInst;
        fromInst = from == null ? Instant.EPOCH : from.toInstant(ZoneOffset.UTC);
        toInst = to == null ? Instant.now() : to.toInstant(ZoneOffset.UTC);
        List<Message> messages = messageService.getMessages(chatId, fromInst, toInst);
        return new ResponseEntity<>(messages, HttpStatus.OK);
    }

    @PostMapping
    ResponseEntity<?> createMessage(@PathVariable long chatId, @PathVariable long userId,
                                    @RequestBody Message message) {
        if (chatService.getChats(userId).stream().noneMatch(chat -> chat.getId() == chatId)) {
            throw new AuthorizedAccessException();
        }
        message = messageService.createMessage(message);
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }
}
