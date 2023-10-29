package com.danver.messengerserver.models;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private String id;
    private long chatId;
    private User author;
    private Instant time;
    private MessageData data;
    private MessageType type;

    public enum MessageType {
        @JsonEnumDefaultValue
        CHAT(1),
        JOIN(2),
        LEAVE(3);

        private final byte value;
        private static final Map<Byte, MessageType> map = new HashMap<>();
        static {
            for (MessageType mt: EnumSet.allOf(MessageType.class)) {
                map.put(mt.getValue(), mt);
            }
        }

        MessageType(int value) {
            this.value = (byte)value;
        }
        @JsonValue
        byte getValue() {
            return this.value;
        }

        public static MessageType get(byte value) {
            return MessageType.map.get(value);
        }
    }

    public Message(String id, long chatId, User author, String text) {
        this.id = id;
        this.chatId = chatId;
        this.author = author;
        this.data = new MessageData(MessageDataType.TEXT, text);
        this.type = MessageType.CHAT;
    }

    public Message(String id, long chatId, User author, Instant creationTime, String text) {
        this.id = id;
        this.chatId = chatId;
        this.author = author;
        this.time = creationTime;
        this.data = new MessageData(MessageDataType.TEXT, text);
        this.type = MessageType.CHAT;
    }

}
