package com.danver.messengerserver.controllers;

import com.danver.messengerserver.auth.AuthDTO;
import com.danver.messengerserver.auth.JwtUtil;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.services.interfaces.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = {"/auth"})
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserController userController;

    @Autowired
    public AuthController(UserService userService, JwtUtil jwtUtil, AuthenticationManager authenticationManager, UserController userController) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userController = userController;
    }

    @GetMapping("/")
    String getInfo() {
        return "This is info string";
    }

    @PostMapping("/register")
    ResponseEntity<?> registerUser(@RequestBody User user) {
        return this.userController.createUser(user);
    }

    @PostMapping(value = {"/login"})
    ResponseEntity<?> authenticateUser(HttpServletRequest request,
                                           HttpServletResponse response,
                                           @RequestHeader HttpHeaders headers,
                                           @RequestBody AuthDTO authDTO) {
        // TODO: prevent from repeated authentication
        User user = userService.getUserByEmail(authDTO.getLogin());
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        try {
            Authentication auth = authenticationManager.authenticate
                    (new UsernamePasswordAuthenticationToken(authDTO.getLogin(), authDTO.getPassword()));
            // TODO: fulfill auth with JWT-token only
            SecurityContextHolder.getContext().setAuthentication(auth);
            String token = jwtUtil.generateToken(user);
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return ResponseEntity.ok().headers(headers).build();
        } catch (AuthenticationException e) {
           return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/logout")
    ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.getContext().setAuthentication(null);
        if (response.containsHeader(HttpHeaders.AUTHORIZATION)) {
            response.setHeader(HttpHeaders.AUTHORIZATION, null);
        }
        return ResponseEntity.ok().build();
    }

}
