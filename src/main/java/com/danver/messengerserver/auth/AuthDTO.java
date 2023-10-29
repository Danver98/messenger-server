package com.danver.messengerserver.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class AuthDTO {

    @JsonAlias({"email"})
    private String login;
    private String password;
    private String token;
}
