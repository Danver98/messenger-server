package com.danver.messengerserver.controllers;

import com.danver.messengerserver.MessengerServerApplication;
import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.models.MessageDTO;
import com.danver.messengerserver.services.interfaces.ChatService;
import com.danver.messengerserver.services.interfaces.MessageService;
import com.danver.messengerserver.utils.Constants;
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

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(path = "/messages")
public class MessageController {

    private final static Long timeoutValue = 5000L;
    private final static int timeoutResult = 1000;
    private final MessageService messageService;
    private final ChatService chatService;

    private final SimpMessagingTemplate messagingTemplate;
    private static final Logger logger = LoggerFactory.getLogger(MessengerServerApplication.class.getName());

    @Autowired
    public MessageController(MessageService messageService, ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.chatService = chatService;
        this.messagingTemplate= messagingTemplate;
    }

    @GetMapping("/secret")
    String getSecret() {
        return "This is a secret message!";
    }

    @GetMapping("/{chatId}")
    DeferredResult<ResponseEntity<List<Message>>> getMessagesByDate(@PathVariable long chatId,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                    @RequestParam(required = false,defaultValue ="false")
                                                    Boolean firstLoad, // Whether to load messages at once or to wait
                                                    @RequestParam(required = false) Integer lastCount) {
        // TODO: check access
//        if (!chatService.userInChat(userId, chatId)) {
//            throw new AuthorizedAccessException();
//        }
        from = from == null ? Instant.EPOCH : from.atOffset(ZoneOffset.UTC).toInstant();
        to = to == null ? Instant.now() : to.atOffset(ZoneOffset.UTC).toInstant();
        DeferredResult<ResponseEntity<List<Message>>> deferredResult = new DeferredResult<>(timeoutValue, timeoutResult);
        List<Message> messages = messageService.getMessages(chatId, from, to);
        deferredResult.setResult(new ResponseEntity<>(messages, HttpStatus.OK));
        return deferredResult;
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
                                                                   Integer count)
    {
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


    @MessageMapping("/chat/send-message")
    ResponseEntity<?> createMessage(@Payload MessageDTO messageDTO) {
        // TODO: check user can send message
        String time = new SimpleDateFormat("HH:mm").format(new Date());
        String message = messageDTO.getTestAuthor() + ": " + messageDTO.getTestText() + " (" + time + ")";
        messagingTemplate.convertAndSend( Constants.WS_MESSAGE_SERVICE_CHAT_QUEUE_NAME, message);
        return new ResponseEntity<>(message, HttpStatus.OK);
        //Message message = messageDTO.getMessage();
//        if (!chatService.userInChat(message.getAuthorId(), message.getChatId())) {
//            throw new AuthorizedAccessException();
//        }
        //message = messageService.createMessage(message);
        // If chat is private, send to certain user; else send to everybody in the queue/topic
        //if (messageDTO.chatIsPrivate()) {
//            messagingTemplate.convertAndSendToUser(String.valueOf(messageDTO.getReceiverId()),
//                    message.getChatId() + "/user/queue/messages", message);
            //TODO: full destination should look like 'chat/{chatId}/private/queue/messages
            // As possible solution we can send to certain user, but that'll lead to duplication of channels, won't it?
            //messagingTemplate.convertAndSend("/chat/" + message.getChatId() + Constants.WS_MESSAGE_SERVICE_PRIVATE_CHAT_QUEUE_NAME, message);
            //messagingTemplate.convertAndSend("/chat/" + Constants.WS_MESSAGE_SERVICE_PRIVATE_CHAT_QUEUE_NAME, message);
        //} else {
            //TODO: full destination should look like 'chat/{chatId}/queue/messages
           // messagingTemplate.convertAndSend( "/chat/" + message.getChatId() + Constants.WS_MESSAGE_SERVICE_CHAT_QUEUE_NAME, message);
            //messagingTemplate.convertAndSend("/chat/" + Constants.WS_MESSAGE_SERVICE_PRIVATE_CHAT_QUEUE_NAME, message);
        //}
        //return new ResponseEntity<>(message, HttpStatus.CREATED);
    }

    @MessageMapping("/chat/add-user")
    public void addUser(@Payload MessageDTO messageDTO) {
    }

    @Scheduled(fixedRate = 60000)
    void clearActiveChats() {
        logger.info("ClearActiveChats task running...");
    }
}
