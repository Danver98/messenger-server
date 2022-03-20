package com.danver.messengerserver.models;

public class MessageData {
    MessageDataType type;
    Object value;

    private MessageData() {

    }

    public MessageData(MessageDataType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public MessageDataType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }
}
