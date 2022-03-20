package com.danver.messengerserver.models;

import java.util.List;
import java.util.Objects;

public class Chat {
    private long id;
    private String name;
    private String avatarUrl;
    private List<User> participants;
    private List<Message> messages;

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public List<User> getParticipants() {
        return participants;
    }

    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return id == chat.id && name.equals(chat.name) && Objects.equals(avatarUrl, chat.avatarUrl) && participants.equals(chat.participants) && messages.equals(chat.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, avatarUrl, participants, messages);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setParticipants(List<User> participants) {
        this.participants = participants;
    }
}
