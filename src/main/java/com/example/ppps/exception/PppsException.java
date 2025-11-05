// REVIEW: full code review requested

package com.example.ppps.exception;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PppsException extends RuntimeException {
    private final HttpStatus status;
    private final String message;
}