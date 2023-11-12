package com.danver.messengerserver.controllers;

import com.danver.messengerserver.models.User;
import com.danver.messengerserver.models.UserRequestDTO;
import com.danver.messengerserver.services.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    ResponseEntity<User> getUser(@PathVariable long id) {
        User user = this.userService.getUser(id);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping("/")
    ResponseEntity<List<User>> list(@RequestBody UserRequestDTO dto) {
        List<User> found = this.userService.list(dto);
        return new ResponseEntity<>(found, HttpStatus.OK);
        //return new ResponseEntity<>(found, !found.isEmpty() ? HttpStatus.OK : HttpStatus.NO_CONTENT);
    }

    @PostMapping
    ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = this.userService.createUser(user);
        if (createdUser == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping
    @PatchMapping
    ResponseEntity<?> updateUser(@RequestBody User user) {
        this.userService.updateUser(user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
