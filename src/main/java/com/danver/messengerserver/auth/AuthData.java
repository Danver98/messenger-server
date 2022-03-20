package com.danver.messengerserver.auth;

import com.fasterxml.jackson.annotation.JsonAlias;

public class AuthData {

    @JsonAlias({"email"})
    private String login;
    private String password;
    private String token;

    private AuthData() {

    }

    public AuthData(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public AuthData(String login, String password, String token) {
        this.login = login;
        this.password = password;
        this.token = token;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
