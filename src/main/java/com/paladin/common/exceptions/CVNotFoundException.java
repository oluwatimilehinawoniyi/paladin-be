package com.paladin.common.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CVNotFoundException extends RuntimeException {
    public CVNotFoundException(String message) {
        super(message);
    }
}