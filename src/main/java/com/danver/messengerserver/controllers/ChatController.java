package com.danver.messengerserver.controllers;

import com.danver.messengerserver.MessengerServerApplication;
import com.danver.messengerserver.exceptions.StorageException;
import com.danver.messengerserver.models.*;
import com.danver.messengerserver.services.interfaces.ChatService;
import com.danver.messengerserver.services.interfaces.MessageService;
import com.danver.messengerserver.services.interfaces.StorageService;
import com.danver.messengerserver.utils.Constants;
import com.danver.messengerserver.utils.FileStorageOptions;
import com.danver.messengerserver.utils.FileUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/chats")
public class ChatController {
    /*
        TODO: We should take user from a context
        TODO: check rights!!!
     */

    private final ChatService chatService;
    private final MessageService messageService;
    private final StorageService storageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(MessengerServerApplication.class.getName());

    @Autowired
    public ChatController(ChatService chatService, MessageService messageService, @Qualifier("s3Storage") StorageService storageService, SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.messageService = messageService;
        this.storageService = storageService;
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
    List<Chat> list(@RequestBody ChatPagingDTO dto) {
        // TODO: Check rights
        return chatService.getChats(dto);
    }

    @GetMapping("/{id}")
    Chat getChat(@RequestParam long userId, @PathVariable long id) {
        return chatService.getChat(id);
    }

    @PostMapping("/create")
    Chat createChat(@RequestBody Chat chat) {
        // TODO: use
        return chatService.createChat(chat);
    }

    @PutMapping("/{id}")
    void updateChat(@RequestParam long userId, @RequestBody Chat chat) {
        chatService.updateChat(chat);
    }

    @DeleteMapping("/{id}")
    void deleteChat(@RequestParam long userId, @PathVariable long id) {
        // TODO: publish event
        chatService.deleteChat(id);
    }

    @GetMapping("/{id}/participants")
    List<User> getListOfParticipants(@RequestParam long userId, @PathVariable long id) {
        return chatService.getParticipants(id);
    }

    @PostMapping("/add")
    ResponseEntity<?> addParticipants(@RequestBody ChatRequestDTO dto) {
        // if authorized
        this.chatService.addParticipants(dto.getChatId(), dto.getUsers());
        return ResponseEntity.ok().build();
    }

    /**
     * Method for uploading attachments. Used in conjunction with sendMessage/sendMessage private.
     * It's basically adjusted for uploading images only and needs further development to support other formats
     * @param file
     * @return url of a created resource or null if failed
     */
    @PostMapping("/attachment")
    ResponseEntity<?> createAttachment(@RequestParam MultipartFile file,
                                       @RequestParam Long userId,
                                       @RequestParam Long chatId) {
        if (FileUtils.isImage(file) || FileUtils.isAudio(file) || FileUtils.isVideo(file) ||
            FileUtils.isValid(file)) {
            try {
                String path = "chats/" + chatId + "/attachments";
                FileStorageOptions options = FileStorageOptions
                        .builder()
                        .owner(userId)
                        .path(path)
                        .build();
                String url = storageService.store(file, options);
                return new ResponseEntity<>(url, HttpStatus.OK);
            } catch (StorageException e) {
                return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return null;
    }

    /*
        Former MessageController
     */

    @PostMapping("/{id}/messages")
    ResponseEntity<List<Message>> getMessagesPaged(@PathVariable long id, @RequestBody MessageRequestDTO dto) {
        List<Message> messages = messageService.getMessages(dto);
        return new ResponseEntity<>(messages, HttpStatus.OK);
    }

    @PostMapping("/{id}/messages/create")
    ResponseEntity<?> createMessageHTTP(@PathVariable long id, @RequestBody Message dto) {
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
