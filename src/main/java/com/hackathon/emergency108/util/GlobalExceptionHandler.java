package com.hackathon.emergency108.util;

import com.hackathon.emergency108.auth.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<?> handleUnauthenticated(
            UnauthenticatedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // ✅ 401
                .body(Map.of(
                        "message", ex.getMessage(),
                        "path", request.getRequestURI()
                ));
    }

    @ExceptionHandler(UserBlockedException.class)
    public ResponseEntity<?> handleBlocked(
            UserBlockedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "message", ex.getMessage(),
                        "path", request.getRequestURI()
                ));
    }

    @ExceptionHandler(DriverNotVerifiedException.class)
    public ResponseEntity<?> handleDriverNotVerified(
            DriverNotVerifiedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "message", ex.getMessage(),
                        "path", request.getRequestURI()
                ));
    }

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
