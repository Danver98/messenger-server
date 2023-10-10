package com.danver.messengerserver.models;

import java.time.Instant;
import java.util.List;

public class Chat {
    private long id;
    private String name;
    private String avatarUrl;
    private Instant lastChanged;

    private boolean isPrivate;
    private List<User> participants;
    private List<Message> messages;

    public Chat() {

    }

    public Chat(long id, String name, String avatarUrl, Instant lastChanged, boolean isPrivate) {
        this.id = id;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.lastChanged = lastChanged;
        this.isPrivate = isPrivate;
    }

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

    public void setName(String name) {
        this.name = name;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setParticipants(List<User> participants) {

        this.participants = participants;
        if (this.participants.size() > 2) {
            this.setPrivate(true);
        }
    }

    public Instant getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(Instant lastChanged) {
        this.lastChanged = lastChanged;
    }

    public boolean isPrivate() {

        if (this.participants != null && this.participants.size() > 2) {
            return true;
        }
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }
}
