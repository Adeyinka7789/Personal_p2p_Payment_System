package com.example.ppps.exception;

public class InsufficientFundsException extends TransferException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}