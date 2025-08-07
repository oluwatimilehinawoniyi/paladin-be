package com.paladin.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
public class CannotSendMailException extends RuntimeException {
    public CannotSendMailException(String message, Exception e) {
        super(message);
    }
}