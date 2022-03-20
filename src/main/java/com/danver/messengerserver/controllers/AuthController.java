package com.danver.messengerserver.controllers;

import com.danver.messengerserver.auth.AuthData;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.services.interfaces.MessengerAuthService;
import com.danver.messengerserver.services.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = {"/authenticate", "/login"})
public class AuthController {

    private final UserService userService;
    private final MessengerAuthService messengerAuthService;

    @Autowired
    public AuthController(UserService userService, MessengerAuthService messengerAuthService) {
        this.userService = userService;
        this.messengerAuthService = messengerAuthService;
    }

    @GetMapping("/")
    String getInfo() {
        return "This is info string";
    }

    @PostMapping
    ResponseEntity<User> authenticateUser(@RequestHeader HttpHeaders headers, @RequestBody AuthData authData) {
        User user = userService.getUserByEmail(authData.getLogin());
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        String token = messengerAuthService.authenticateUser(authData, user);
        if (token == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return ResponseEntity.ok().headers(headers).body(user);
    }
}
