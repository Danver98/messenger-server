package com.danver.messengerserver.models;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class MessageDTO {

    private Message message;
    private boolean chatIsPrivate;
    private Long receiverId;
    private String testText;
    private String testAuthor;
}
