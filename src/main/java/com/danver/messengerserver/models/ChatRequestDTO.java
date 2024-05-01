package com.danver.messengerserver.models;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class ChatRequestDTO {
    long chatId;
    long userId;
    String messageId;
    long[] users;
}
