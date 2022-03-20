package com.danver.messengerserver.utils;

public enum Constants {

    USER_JWT_EMAIL_KEY("email");

    private final String value;

    Constants(String value) {
        this.value = value;
    }

    public String getValue(){
        return this.value;
    }
}
