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
    private long receiverId;
    private Instant time;
    private MessageData data;
    private MessageType type;

    public enum MessageType {
        @JsonEnumDefaultValue
        CHAT(1),
        JOIN(2),
        INVITATION(3),
        LEAVE(4);

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

}
