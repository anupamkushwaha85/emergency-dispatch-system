package com.hackathon.emergency108.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidAssignmentStateException extends RuntimeException {
    public InvalidAssignmentStateException(String message) {
        super(message);
    }
}
