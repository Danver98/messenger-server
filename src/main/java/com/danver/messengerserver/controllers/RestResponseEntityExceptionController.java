package com.danver.messengerserver.controllers;

import com.danver.messengerserver.exceptions.AuthorizedAccessException;
import com.danver.messengerserver.exceptions.CompletableFutureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;

@Slf4j
@ControllerAdvice
public class RestResponseEntityExceptionController extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value
            = {AuthorizedAccessException.class})
    public ResponseEntity<Object> authAccessException(AuthorizedAccessException exception) {
        return new ResponseEntity<>("Access not allowed", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(value = {DataAccessException.class, SQLException.class})
    public ResponseEntity<Object> dataAccessException(DataAccessException exception) {
        log.error(exception.getMessage());
        return new ResponseEntity<>("Couldn't access the data source", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {CompletableFutureException.class})
    public ResponseEntity<Object> taskExecutionException(CompletableFutureException exception) {
        log.error(exception.getMessage());
        return new ResponseEntity<>("Error occurred when completing task", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
