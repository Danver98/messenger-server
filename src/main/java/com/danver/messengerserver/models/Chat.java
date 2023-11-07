package com.danver.messengerserver.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

public class Chat {
    @Getter
    private long id;
    @Getter
    private String name;
    @Getter
    @JsonProperty("avatar")
    @JsonAlias("avatar")
    private String avatarUrl;
    @JsonProperty("time")
    @JsonAlias("time")
    private Instant lastChanged;

    @Setter
    private boolean isPrivate;
    @Getter
    private List<User> participants;
    @Getter
    private List<Message> messages;
    @Getter
    private Message lastMessage;

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

    public void setLastChanged(Instant lastChanged) {
        this.lastChanged = lastChanged;
    }

    public boolean isPrivate() {

        if (this.participants != null && this.participants.size() > 2) {
            return true;
        }
        return isPrivate;
    }

    public void setLastMessage(Message lastMessage) {
        this.lastMessage = lastMessage;
    }
}
