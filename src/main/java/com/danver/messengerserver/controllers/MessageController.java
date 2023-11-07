package com.danver.messengerserver.controllers;

import com.danver.messengerserver.MessengerServerApplication;
import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.models.MessageDTO;
import com.danver.messengerserver.models.MessageRequestDTO;
import com.danver.messengerserver.services.interfaces.ChatService;
import com.danver.messengerserver.services.interfaces.MessageService;
import com.danver.messengerserver.utils.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.time.Instant;
import java.util.List;

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
    public MessageController(MessageService messageService, ChatService chatService, SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/secret")
    String getSecret() {
        return "This is a secret message!";
    }

    @GetMapping("/paged/{chatId}")
    DeferredResult<ResponseEntity<List<Message>>> getMessagesPaged(@PathVariable long chatId,
                                                                   @RequestParam(required = false)
                                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                                   Instant before,
                                                                   @RequestParam(required = false)
                                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                                   Instant after,
                                                                   @RequestParam(required = false)
                                                                   String cursorMsgId,
                                                                   @RequestParam(required = false)
                                                                   Integer count) {
        // TODO: - dispose of DB call for each check (preferably use Redis);
        // TODO (optionally): extract this logic to filter/interceptors level
        // TODO: check access
//        if (!chatService.userInChat(userId, chatId)) {
//            throw new AuthorizedAccessException();
//        }

        DeferredResult<ResponseEntity<List<Message>>> deferredResult = new DeferredResult<>(timeoutValue, timeoutResult);
        List<Message> messages = messageService.getMessagesPaged(chatId, before, after, cursorMsgId, count);
        deferredResult.setResult(new ResponseEntity<>(messages, HttpStatus.OK));
        return deferredResult;
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

    @MessageMapping("/chats/private/send-message")
    ResponseEntity<?> sendMessagePrivate(@Payload MessageDTO dto) {
        // TODO: check authority
        try {
            messageService.createMessage(dto.getMessage());
            messagingTemplate.convertAndSendToUser(
                    Long.toString(dto.getMessage().getReceiverId()),
                    Constants.MESSAGE_BROKER_QUEUE_PREFIX + "/chats/messages",
                    dto.getMessage());
            return new ResponseEntity<>(dto.getMessage(), HttpStatus.OK);
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
            messageService.createMessage(dto.getMessage());
            messagingTemplate.convertAndSend(
                    Constants.MESSAGE_BROKER_TOPIC_PREFIX + "/chats/" + dto.getMessage().getChatId() + "/messages",
                    dto.getMessage()
                    );
            return new ResponseEntity<>(dto.getMessage(), HttpStatus.OK);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @MessageMapping("/chat/add-user")
    public void addUser(@Payload MessageDTO messageDTO) {
    }

    @Scheduled(fixedRate = 60000)
    void clearActiveChats() {
        logger.info("ClearActiveChats task running...");
    }
}
