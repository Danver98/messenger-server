package com.danver.messengerserver.models;

import lombok.Getter;

public class MessageDTO {

    @Getter
    private Message message;
    private boolean chatIsPrivate;

    private Long receiverId;

    @Getter
    private String testText;
    @Getter
    private String testAuthor;

    public MessageDTO() {

    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public boolean chatIsPrivate() {
        return chatIsPrivate;
    }

    public void setChatIsPrivate(boolean chatIsPrivate) {
        this.chatIsPrivate = chatIsPrivate;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public void setTestText(String testText) {
        this.testText = testText;
    }

    public void setTestAuthor(String testAuthor) {
        this.testAuthor = testAuthor;
    }
}
