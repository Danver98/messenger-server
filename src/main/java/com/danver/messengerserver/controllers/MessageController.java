package com.danver.messengerserver.controllers;

import com.danver.messengerserver.MessengerServerApplication;
import com.danver.messengerserver.exceptions.StorageException;
import com.danver.messengerserver.models.*;
import com.danver.messengerserver.services.interfaces.ChatService;
import com.danver.messengerserver.services.interfaces.MessageService;
import com.danver.messengerserver.services.interfaces.StorageService;
import com.danver.messengerserver.utils.Constants;
import com.danver.messengerserver.utils.FileStorageOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.security.Principal;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(path = "/messages")
public class MessageController {

    private final static Long timeoutValue = 5000L;
    private final static int timeoutResult = 1000;
    private final MessageService messageService;
    private final ChatService chatService;

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(MessengerServerApplication.class.getName());

    @Autowired
    public MessageController(MessageService messageService, ChatService chatService,
                             SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/secret")
    String getSecret(Authentication authentication,
                     Principal principal,
                    @AuthenticationPrincipal UserDetails userDetails) {
        return "This is a secret message!";
    }

    // TODO: use @Header('sessionId') or @SendToUser (less preferable) for security
    // Maybe we should split methods to send data either to private chat or to group chat (look https://www.baeldung.com/spring-websockets-send-message-to-user)


    @PostMapping("/")
    ResponseEntity<List<Message>> getMessagesPaged(@RequestBody MessageRequestDTO dto) {
        // TODO: check authority
        List<Message> messages = messageService.getMessages(dto);
        return new ResponseEntity<>(messages, HttpStatus.OK);
    }

    @PostMapping("/create")
    ResponseEntity<?> createMessageHTTP(@RequestBody Message dto) {
        try {
            Message message = messageService.createMessage(dto);
            return new ResponseEntity<>(message, HttpStatus.OK);
        } catch (Exception e) {

        }
        return null;
    }


    @MessageMapping("/chats/create-invite")
    ResponseEntity<?> createChat(@Payload String messageDTO) {
        // TODO: check authority
        try {
            MessageDTO dto = objectMapper.readValue(messageDTO, MessageDTO.class);
            Message message = dto.getMessage();
            if (message.getType() != Message.MessageType.CREATION) {
                return null;
            }
            Long [] participants;
            Chat chat = dto.getChat();
            if (chat == null) {
                List<User> chatUsers = chatService.getParticipants(message.getChatId());
                if (chatUsers == null) {
                    return null;
                }
                participants = chatUsers.stream().map(User::getId)
                        .toArray(Long[]::new);
            } else {
                participants = chat.getParticipants()
                        .toArray(Long[]::new);
            }
            Message created = messageService.createMessage(dto.getMessage());
            dto.setMessage(created);
            String destination = Constants.MESSAGE_BROKER_QUEUE_PREFIX + "/chats/messages";
            for (long user: participants) {
                if (chat != null && chat.isPrivate() && user == message.getAuthor().getId()) {
                    // Exclude message author from receivers list, if private chat given
                    continue;
                }
                messagingTemplate.convertAndSendToUser(
                        Long.toString(user),
                        destination,
                        dto
                );
            }
            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @MessageMapping("/chats/private/send-message")
    ResponseEntity<?> sendMessagePrivate(@Payload String messageDTO) {
        // TODO: check authority
        try {
            MessageDTO dto = objectMapper.readValue(messageDTO, MessageDTO.class);
            Message message = messageService.createMessage(dto.getMessage());
            dto.setMessage(message);
            String destination = Constants.MESSAGE_BROKER_QUEUE_PREFIX + "/chats/messages";
            messagingTemplate.convertAndSendToUser(
                    Long.toString(dto.getMessage().getReceiverId()),
                    destination,
                    dto
            );
            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @MessageMapping("/chats/public/send-message")
    ResponseEntity<?> sendMessage(@Payload String messageDTO) {
        // Unfortunately cannot find a workaround to pass MessageDTO directly
        // TODO: check authority
        // TODO: append auxiliary info (ex. author's avatar url, name, surname) if absent
        try {
            MessageDTO dto = objectMapper.readValue(messageDTO, MessageDTO.class);
            Message message = messageService.createMessage(dto.getMessage());
            dto.setMessage(message);
            messagingTemplate.convertAndSend(
                    Constants.MESSAGE_BROKER_TOPIC_PREFIX + "/chats/" + dto.getMessage().getChatId() + "/messages",
                    dto
                    );
            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @MessageMapping("/chat/add-user")
    public void addUser(@Payload MessageDTO messageDTO) {
    }


    @Scheduled(fixedRate = 60000)
    void clearActiveChats() {
        logger.info("ClearActiveChats task running: thread id - " + Thread.currentThread().getId() +
                ", thread name - " + Thread.currentThread().getName());
    }
}
