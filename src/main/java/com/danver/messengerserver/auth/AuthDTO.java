package com.danver.messengerserver.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;

@Getter
public class AuthDTO {

    @JsonAlias({"email"})
    private String login;
    private String password;
    private String token;

    private AuthDTO() {

    }

    public AuthDTO(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public AuthDTO(String login, String password, String token) {
        this.login = login;
        this.password = password;
        this.token = token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
