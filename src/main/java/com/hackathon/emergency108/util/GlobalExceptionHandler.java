package com.hackathon.emergency108.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidAssignmentStateException.class)
    public ResponseEntity<?> handleInvalidAssignment(
            InvalidAssignmentStateException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "message", ex.getMessage(),
                        "path", request.getRequestURI()
                ));
    }
}
