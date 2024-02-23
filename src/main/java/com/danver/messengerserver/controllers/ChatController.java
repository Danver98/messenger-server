package com.danver.messengerserver.controllers;

import com.danver.messengerserver.MessengerServerApplication;
import com.danver.messengerserver.exceptions.AuthorizedAccessException;
import com.danver.messengerserver.exceptions.StorageException;
import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.ChatPagingDTO;
import com.danver.messengerserver.models.ChatRequestDTO;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.services.interfaces.ChatService;
import com.danver.messengerserver.services.interfaces.StorageService;
import com.danver.messengerserver.utils.FileStorageOptions;
import com.danver.messengerserver.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/chats")
public class ChatController {
    /*
        TODO: We should take user from a context
        TODO: check rights!!!
     */

    private final ChatService chatService;
    private final StorageService storageService;

    private static final Logger logger = LoggerFactory.getLogger(MessengerServerApplication.class.getName());

    @Autowired
    public ChatController(ChatService chatService, @Qualifier("s3Storage") StorageService storageService) {
        this.chatService = chatService;
        this.storageService = storageService;
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
        // TODO: use
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
}
