package com.example.ppps.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@Data
@EqualsAndHashCode(callSuper = false)  // <-- ADDED THIS to suppress warning
public class PppsException extends RuntimeException {
    private final HttpStatus status;
    private final String message;

    public PppsException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.message = message;
    }
}