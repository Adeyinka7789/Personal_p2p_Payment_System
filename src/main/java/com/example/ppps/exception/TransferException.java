// REVIEW: full code review requested

package com.example.ppps.exception;

public class TransferException extends RuntimeException {
    public TransferException(String message) {
        super(message);
    }
}