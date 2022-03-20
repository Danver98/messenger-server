package com.danver.messengerserver.models;

import java.time.Instant;
import java.util.Objects;

public class Message {
    private String id;
    private long chatId;
    private User author;

    private Instant creationTime;
    private MessageData data;

    public Message() {

    }

    public Message(String id, long chatId, User author, String text) {
        this.id = id;
        this.chatId = chatId;
        this.author = author;
        this.data = new MessageData(MessageDataType.TEXT, text);
    }

    public Message(String id, long chatId, User author, Instant creationTime, String text) {
        this.id = id;
        this.chatId = chatId;
        this.author = author;
        this.creationTime = creationTime;
        this.data = new MessageData(MessageDataType.TEXT, text);
    }

    public Message(String id, long chatId, User author, Instant creationTime, MessageData data) {
        this.id = id;
        this.chatId = chatId;
        this.author = author;
        this.creationTime = creationTime;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public long getChatId() {
        return chatId;
    }

    public User getAuthor() {
        return author;
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public MessageData getData() {
        return data;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public void setCreationTime(Instant creationTime) {
        this.creationTime = creationTime;
    }

    public void setData(MessageData data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return id.equals(message.id) && chatId == message.chatId && author.equals(message.author) && creationTime.equals(message.creationTime) && data.equals(message.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, chatId, author, creationTime, data);
    }
}
