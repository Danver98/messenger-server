package com.danver.messengerserver.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is a temporal controller for test purposes only
 */
@RestController()
@RequestMapping("/public")
public class PublicTestController {

    @GetMapping("/long-task")
    public ResponseEntity<?> runLongTask() {
        try {
            long time = 3000L;
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }
}
