package com.ronak.welcome.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // This makes it return a 400 HTTP status
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
